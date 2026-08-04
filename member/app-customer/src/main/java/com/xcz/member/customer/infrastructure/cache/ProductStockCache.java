package com.xcz.member.customer.infrastructure.cache;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.enums.Constants;
import lombok.experimental.UtilityClass;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 商品库存 Redis 缓存工具类（高并发扣减场景）。
 * <p>
 * 基于 Redisson 维护三类数据：
 * <ul>
 *   <li>实时剩余库存（{@code product:stock:{id}}）</li>
 *   <li>Redis 侧累计销量（{@code product:sold:pending:{id}}），定时任务回写数据库</li>
 *   <li>待同步商品集合（{@code product:sold:dirty}）</li>
 * </ul>
 * 扣减路径先改 Redis，再由 {@link com.xcz.member.customer.infrastructure.task.service.ProductSoldSyncService}
 * 将剩余库存与累计销量同步至数据库；商品写操作缓存维护见
 * {@link com.xcz.member.customer.infrastructure.service.MobiShopProductServiceImpl}。
 */
@UtilityClass
public class ProductStockCache {

    /** 无限库存标记值，与 {@link CouponStockCache#UNLIMITED_STOCK} 一致 */
    public static final long UNLIMITED_STOCK = -1L;

    private static final RedissonClient REDISSON = Constants.CACHE;
    /** 商品实时剩余库存：product:stock:{productId} */
    private static final String STOCK_KEY = "product:stock:";
    /** Redis 侧累计销量（含待回写 DB 的增量）：product:sold:pending:{productId} */
    private static final String SOLD_PENDING_KEY = "product:sold:pending:";
    /** 存在待同步销量的商品 ID 集合：product:sold:dirty */
    private static final String DIRTY_PRODUCTS_KEY = "product:sold:dirty";
    /** 单商品库存操作分布式锁：product:lock:{productId} */
    private static final String STOCK_LOCK = "product:lock:";
    /** 库存 key 过期时间，防止冷数据常驻内存 */
    private static final Duration CACHE_TTL_DAYS = Duration.ofDays(7);

    /**
     * 获取指定商品的库存原子对象。
     *
     * @param productId 商品 ID
     * @return 对应商品的 {@link RAtomicLong} 实例
     */
    private static RAtomicLong stockAtomic(Long productId) {
        return REDISSON.getAtomicLong(STOCK_KEY + productId);
    }

    /**
     * 获取指定商品的 Redis 侧累计销量原子对象。
     *
     * @param productId 商品 ID
     * @return 对应商品的 {@link RAtomicLong} 实例
     */
    private static RAtomicLong soldPendingAtomic(Long productId) {
        return REDISSON.getAtomicLong(SOLD_PENDING_KEY + productId);
    }

    /**
     * 判断商品库存缓存是否尚未初始化。
     *
     * @param productId 商品 ID
     * @return 库存 key 不存在时返回 {@code true}
     */
    public boolean isNotExists(Long productId) {
        return !stockAtomic(productId).isExists();
    }

    /**
     * 基于分布式锁执行有返回值的操作。
     *
     * @param productId 商品 ID，作为锁粒度
     * @param supplier  需要在锁保护下执行的操作
     * @param <T>       返回值类型
     * @return supplier 的执行结果
     * @throws ServiceException 获取锁失败时抛出
     */
    private <T> T lock(Long productId, Supplier<T> supplier) {
        RLock lock = REDISSON.getLock(STOCK_LOCK + productId);
        boolean getLock = false;
        try {
            getLock = lock.tryLock(500, -1, TimeUnit.MILLISECONDS);
            if (!getLock) {
                throw new ServiceException("获取库存锁失败，请稍后重试");
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取库存锁被中断", e);
        } finally {
            if (getLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 基于分布式锁执行无返回值的操作。
     *
     * @param productId 商品 ID，作为锁粒度
     * @param runnable  需要在锁保护下执行的操作
     */
    private void lock(Long productId, Runnable runnable) {
        lock(productId, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 对多个商品按 ID 升序加联锁，避免购物车跨商品扣减时的死锁与中间态窗口。
     *
     * @param productIds 参与联锁的商品 ID 集合
     * @param supplier   需要在联锁保护下执行的操作
     * @param <T>        返回值类型
     * @return supplier 的执行结果
     * @throws ServiceException 获取锁失败时抛出
     */
    private <T> T multiLock(Collection<Long> productIds, Supplier<T> supplier) {
        List<Long> sorted = productIds.stream().sorted().distinct().toList();
        if (sorted.isEmpty()) {
            return supplier.get();
        }
        if (sorted.size() == 1) {
            return lock(sorted.getFirst(), supplier);
        }
        RLock[] locks = sorted.stream()
                .map(id -> REDISSON.getLock(STOCK_LOCK + id))
                .toArray(RLock[]::new);
        RLock multiLock = REDISSON.getMultiLock(locks);
        boolean getLock = false;
        try {
            getLock = multiLock.tryLock(500, -1, TimeUnit.MILLISECONDS);
            if (!getLock) {
                throw new ServiceException("获取库存锁失败，请稍后重试");
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取库存锁被中断", e);
        } finally {
            if (getLock && multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    /**
     * 初始化商品库存缓存。
     * <p>
     * 仅在对应 key 不存在时写入：待同步销量以数据库累计已售为基准；
     * 可用库存 = 数据库剩余库存 −（Redis 累计销量 − 数据库累计已售）中尚未回写的部分。
     *
     * @param productId        商品 ID
     * @param dbRemainingStock 数据库剩余库存
     * @param dbSoldCount      数据库累计已售数量
     */
    public void init(Long productId, long dbRemainingStock, long dbSoldCount) {
        if (productId == null) {
            return;
        }
        lock(productId, () -> {
            RAtomicLong stock = stockAtomic(productId);
            RAtomicLong pending = soldPendingAtomic(productId);
            if (!pending.isExists()) {
                pending.set(dbSoldCount);
                pending.expire(CACHE_TTL_DAYS);
            }
            if (!stock.isExists()) {
                long redisSold = pending.get();
                long unsynced = Math.max(0, redisSold - dbSoldCount);
                long available = dbRemainingStock == UNLIMITED_STOCK
                        ? UNLIMITED_STOCK
                        : Math.max(0, dbRemainingStock - unsynced);
                stock.set(available);
                stock.expire(CACHE_TTL_DAYS);
            }
        });
    }

    /**
     * 扣减商品库存并累加 Redis 侧累计销量。
     * <p>
     * 在分布式锁保护下执行：先扣减可用库存，不足则回滚并抛异常；
     * 成功后累加待同步销量，并标记为脏数据等待定时任务回写。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量（必须大于 0）
     * @return 扣减后的剩余库存；无限库存时返回 {@link #UNLIMITED_STOCK}
     * @throws ServiceException 参数无效或库存不足时抛出
     */
    public long deductStock(Long productId, long quantity) {
        if (productId == null || quantity <= 0) {
            throw new ServiceException("扣减数量无效");
        }
        return lock(productId, () -> deductStockUnlocked(productId, quantity));
    }

    /**
     * 批量扣减库存：同一订单涉及的商品在联锁保护下整单扣减，任一商品不足则本单全部回滚。
     *
     * @param deductions 扣减明细，每项为 {@code [productId, quantity]}
     * @return 各商品扣减后的剩余库存
     * @throws ServiceException 参数无效或任一商品库存不足时抛出
     */
    public Map<Long, Long> deductStockBatch(List<long[]> deductions) {
        TreeMap<Long, Long> merged = mergeDeductions(deductions);
        if (merged.isEmpty()) {
            return Map.of();
        }
        return multiLock(merged.keySet(), () -> {
            Map<Long, Long> remainingMap = new HashMap<>();
            List<long[]> completed = new ArrayList<>();
            try {
                for (Map.Entry<Long, Long> entry : merged.entrySet()) {
                    long remaining = deductStockUnlocked(entry.getKey(), entry.getValue());
                    completed.add(new long[]{entry.getKey(), entry.getValue()});
                    remainingMap.put(entry.getKey(), remaining);
                }
                return remainingMap;
            } catch (RuntimeException ex) {
                for (int i = completed.size() - 1; i >= 0; i--) {
                    long[] item = completed.get(i);
                    rollbackDeductUnlocked(item[0], item[1]);
                }
                throw ex;
            }
        });
    }

    /**
     * 合并同一商品的重复扣减行，并按商品 ID 升序排列，保证联锁顺序一致。
     *
     * @param deductions 原始扣减明细
     * @return 合并后的商品 ID → 扣减数量
     * @throws ServiceException 明细格式或数量无效时抛出
     */
    private static TreeMap<Long, Long> mergeDeductions(List<long[]> deductions) {
        TreeMap<Long, Long> merged = new TreeMap<>();
        if (deductions == null) {
            return merged;
        }
        for (long[] item : deductions) {
            if (item == null || item.length < 2 || item[0] <= 0 || item[1] <= 0) {
                throw new ServiceException("扣减数量无效");
            }
            merged.merge(item[0], item[1], Long::sum);
        }
        return merged;
    }

    /**
     * 在已持有锁的前提下扣减库存（调用方须保证并发安全）。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 扣减后的剩余库存
     * @throws ServiceException 库存不足时抛出
     */
    private long deductStockUnlocked(Long productId, long quantity) {
        RAtomicLong stock = stockAtomic(productId);
        long remaining = stock.get();
        if (remaining != UNLIMITED_STOCK) {
            remaining = stock.addAndGet(-quantity);
            if (remaining < 0) {
                stock.addAndGet(quantity);
                throw new ServiceException("商品库存不足");
            }
        }

        RAtomicLong pending = soldPendingAtomic(productId);
        pending.addAndGet(quantity);
        markDirty(productId);

        stock.expire(CACHE_TTL_DAYS);
        pending.expire(CACHE_TTL_DAYS);
        return remaining;
    }

    /**
     * 获取商品实时可用库存。
     * <p>
     * 缓存未初始化时返回 0；读操作不加锁以提升并发性能。
     * 调用方应在展示或校验前通过 {@link #init} 或业务层 {@code ensureStockCached} 预热缓存。
     *
     * @param productId 商品 ID
     * @return 实时可用库存（非负）；无限库存时返回 {@link #UNLIMITED_STOCK}
     */
    public long getAvailableStock(Long productId) {
        if (productId == null) {
            return 0L;
        }
        RAtomicLong stock = stockAtomic(productId);
        if (!stock.isExists()) {
            return 0L;
        }
        long value = stock.get();
        return value == UNLIMITED_STOCK ? UNLIMITED_STOCK : Math.max(0, value);
    }

    /**
     * 获取 Redis 侧累计销量（含尚未回写数据库的增量）。
     * <p>
     * 缓存不存在时返回 0；定时任务以此值与剩余库存一并同步至数据库。
     *
     * @param productId 商品 ID
     * @return Redis 侧累计已售数量
     */
    public long getPendingSold(Long productId) {
        if (productId == null) {
            return 0L;
        }
        return soldPendingAtomic(productId).get();
    }

    /**
     * 增加商品可用库存（取消订单、补货等场景）。
     * <p>
     * 在分布式锁保护下累加库存并续期；无限库存商品直接返回当前值。
     *
     * @param productId 商品 ID
     * @param quantity  增加数量（必须大于等于 0）
     * @return 增加后的库存数量
     * @throws ServiceException 参数无效或库存缓存未初始化时抛出
     */
    public Long addStock(Long productId, long quantity) {
        if (productId == null || quantity < 0) {
            throw new ServiceException("增加数量无效");
        }
        return lock(productId, () -> {
            RAtomicLong stock = stockAtomic(productId);
            if (!stock.isExists()) {
                throw new ServiceException("商品库存缓存不存在");
            }
            if (stock.get() != UNLIMITED_STOCK) {
                long newStock = stock.addAndGet(quantity);
                stock.expire(CACHE_TTL_DAYS);
                return newStock;
            }
            return stock.get();
        });
    }

    /**
     * 回滚一次扣减（积分校验失败等场景）：恢复可用库存并减少 Redis 侧累计销量。
     *
     * @param productId 商品 ID
     * @param quantity  回滚数量（小于等于 0 时忽略）
     */
    public void rollbackDeduct(Long productId, long quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        lock(productId, () -> rollbackDeductUnlocked(productId, quantity));
    }

    /**
     * 在已持有锁的前提下回滚扣减（调用方须保证并发安全）。
     *
     * @param productId 商品 ID
     * @param quantity  回滚数量
     */
    private void rollbackDeductUnlocked(Long productId, long quantity) {
        RAtomicLong stock = stockAtomic(productId);
        if (stock.isExists() && stock.get() != UNLIMITED_STOCK) {
            stock.addAndGet(quantity);
            stock.expire(CACHE_TTL_DAYS);
        }
        RAtomicLong pending = soldPendingAtomic(productId);
        if (pending.isExists()) {
            long after = pending.addAndGet(-quantity);
            if (after < 0) {
                pending.set(0);
            }
            pending.expire(CACHE_TTL_DAYS);
        }
    }

    /**
     * 清除指定商品的全部库存相关缓存（剩余库存、累计销量及脏标记）。
     *
     * @param productId 商品 ID
     */
    public void evict(Long productId) {
        if (productId == null) {
            return;
        }
        stockAtomic(productId).delete();
        soldPendingAtomic(productId).delete();
        REDISSON.getSet(DIRTY_PRODUCTS_KEY).remove(productId);
    }

    /**
     * 数据库同步完成后，将商品从脏数据集合中移除。
     *
     * @param productId 商品 ID
     */
    public void clearDirty(Long productId) {
        if (productId != null) {
            REDISSON.getSet(DIRTY_PRODUCTS_KEY).remove(productId);
        }
    }

    /**
     * 获取所有存在待同步销量的商品 ID 集合，供定时任务遍历。
     *
     * @return 脏数据商品 ID 集合（无数据时返回空集合）
     */
    public Set<Long> listDirtyProductIds() {
        RSet<Long> dirtySet = REDISSON.getSet(DIRTY_PRODUCTS_KEY);
        Collection<Long> all = dirtySet.readAll();
        return all == null ? Set.of() : new HashSet<>(all);
    }

    /**
     * 将商品标记为存在待同步销量，等待定时任务回写数据库。
     *
     * @param productId 商品 ID
     */
    private void markDirty(Long productId) {
        REDISSON.getSet(DIRTY_PRODUCTS_KEY).add(productId);
    }
}
