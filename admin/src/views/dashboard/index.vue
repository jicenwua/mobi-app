<template>
  <div class="dashboard-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>数据概览</span>
          <div class="header-filters">
            <el-select
              v-model="selectedShopId"
              clearable
              filterable
              placeholder="全部店铺（系统汇总）"
              class="filter-select"
              @change="onShopChange"
            >
              <el-option
                v-for="shop in shopOptions"
                :key="shop.id"
                :label="shop.shopName"
                :value="shop.id"
              />
            </el-select>
            <el-select
              v-if="scopeOptions.length"
              v-model="selectedScope"
              placeholder="数据范围"
              class="filter-select"
              @change="loadStatistics"
            >
              <el-option
                v-for="opt in scopeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-select
              v-model="periodPreset"
              placeholder="统计时段"
              class="filter-select"
              @change="onPeriodPresetChange"
            >
              <el-option label="今天" value="today" />
              <el-option label="近3天" value="3d" />
              <el-option label="近7天" value="7d" />
              <el-option label="近30天" value="30d" />
              <el-option label="自定义" value="custom" />
            </el-select>
            <el-date-picker
              v-if="periodPreset === 'custom'"
              v-model="customDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              class="filter-date-range"
              :disabled-date="disableFutureDate"
              @change="onCustomDateChange"
            />
          </div>
        </div>
      </template>

      <div class="dashboard-content">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :lg="6">
            <el-statistic :title="statTitles.newUsers" :value="summary.newUsers">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-statistic :title="statTitles.orders" :value="summary.orders">
              <template #prefix>
                <el-icon><ShoppingCart /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-statistic :title="statTitles.pointsUsed" :value="summary.pointsUsed" :precision="0">
              <template #prefix>
                <el-icon><Coin /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-statistic :title="statTitles.rechargeAmount" :value="summary.rechargeAmount" :precision="2">
              <template #prefix>¥</template>
            </el-statistic>
          </el-col>
        </el-row>

        <el-divider />

        <div class="chart-section">
          <div class="chart-title">{{ chartTitle }}</div>
          <div ref="chartRef" class="trend-chart" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { User, ShoppingCart, Coin } from '@element-plus/icons-vue'
import { getShopStatistics } from '@/api/dashboard/statistics'
import { listShop } from '@/api/shop/shop'

const loading = ref(false)
const chartRef = ref(null)
let chartInstance = null

const shopOptions = ref([])
const selectedShopId = ref(null)
const selectedScope = ref('all')
const periodPreset = ref('today')
const customDateRange = ref([])
const statistics = ref(null)

const summary = computed(() => ({
  newUsers: Number(statistics.value?.todayNewUsers ?? 0),
  orders: Number(statistics.value?.todayOrders ?? 0),
  pointsUsed: Number(statistics.value?.totalPointsUsed ?? 0),
  rechargeAmount: Number(statistics.value?.todayRechargeAmount ?? 0)
}))

const isTodayPeriod = computed(() => {
  const { startDate, endDate } = resolveDateRange()
  return startDate === endDate && startDate === formatDate(new Date())
})

const statTitles = computed(() => {
  if (isTodayPeriod.value) {
    return {
      newUsers: '今日新用户数',
      orders: '今日订单数',
      pointsUsed: '今日使用积分',
      rechargeAmount: '今日充值金额'
    }
  }
  return {
    newUsers: '新用户数',
    orders: '订单数',
    pointsUsed: '使用积分',
    rechargeAmount: '充值金额'
  }
})

const chartTitle = computed(() => {
  const { startDate, endDate } = resolveDateRange()
  if (startDate === endDate) {
    return `${startDate} 走势`
  }
  return `${startDate} ~ ${endDate} 走势`
})

const scopeOptions = computed(() => {
  if (!statistics.value?.headShop || !selectedShopId.value) {
    return []
  }
  const options = [
    { label: '总店汇总（含全部分店）', value: 'all' },
    { label: '总店本店', value: `self:${selectedShopId.value}` }
  ]
  for (const branch of statistics.value.branches || []) {
    options.push({
      label: branch.shopName || `分店 #${branch.id}`,
      value: `shop:${branch.id}`
    })
  }
  return options
})

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function resolveDateRange() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  if (periodPreset.value === 'custom') {
    if (Array.isArray(customDateRange.value) && customDateRange.value.length === 2) {
      return {
        startDate: customDateRange.value[0],
        endDate: customDateRange.value[1]
      }
    }
    const todayStr = formatDate(today)
    return { startDate: todayStr, endDate: todayStr }
  }

  const dayMap = {
    today: 1,
    '3d': 3,
    '7d': 7,
    '30d': 30
  }
  const days = dayMap[periodPreset.value] || 1
  const start = new Date(today)
  start.setDate(start.getDate() - (days - 1))
  return {
    startDate: formatDate(start),
    endDate: formatDate(today)
  }
}

function disableFutureDate(date) {
  return date.getTime() > Date.now()
}

function buildQueryParams() {
  const params = {}
  const { startDate, endDate } = resolveDateRange()
  params.startDate = startDate
  params.endDate = endDate

  if (selectedShopId.value) {
    params.shopId = selectedShopId.value
    if (selectedScope.value?.startsWith('self:')) {
      params.filterShopId = selectedShopId.value
    } else if (selectedScope.value?.startsWith('shop:')) {
      params.filterShopId = Number(selectedScope.value.split(':')[1])
    }
  }
  return params
}

async function loadShopOptions() {
  try {
    const res = await listShop({ pageNum: 1, pageSize: 500, auditStatus: 1, isEnabled: 1 })
    shopOptions.value = res.data || []
  } catch {
    shopOptions.value = []
  }
}

async function loadStatistics() {
  loading.value = true
  try {
    const res = await getShopStatistics(buildQueryParams())
    statistics.value = res.data || null
    await nextTick()
    renderChart()
  } catch (e) {
    statistics.value = null
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onShopChange() {
  selectedScope.value = 'all'
  loadStatistics()
}

function onPeriodPresetChange() {
  if (periodPreset.value !== 'custom') {
    customDateRange.value = []
    loadStatistics()
  }
}

function onCustomDateChange(value) {
  if (Array.isArray(value) && value.length === 2) {
    loadStatistics()
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const trend = statistics.value?.trend || []
  const dates = trend.map((item) => item.date)
  chartInstance.setOption(
    {
      tooltip: {
        trigger: 'axis',
        confine: true
      },
      legend: {
        top: 0,
        left: 'center',
        itemGap: 24,
        data: ['新用户', '订单', '使用积分', '充值金额']
      },
      grid: {
        left: 12,
        right: 12,
        top: 56,
        bottom: 8,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: true,
        data: dates,
        axisLabel: {
          margin: 12
        }
      },
      yAxis: [
        {
          type: 'value',
          name: '人数/订单',
          nameGap: 12,
          minInterval: 1,
          splitLine: {
            lineStyle: { type: 'dashed' }
          }
        },
        {
          type: 'value',
          name: '积分/金额',
          position: 'right',
          nameGap: 12,
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '新用户',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: trend.map((item) => item.newUsers ?? 0)
        },
        {
          name: '订单',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: trend.map((item) => item.orders ?? 0)
        },
        {
          name: '使用积分',
          type: 'bar',
          yAxisIndex: 1,
          barMaxWidth: 28,
          data: trend.map((item) => Number(item.pointsUsed ?? 0))
        },
        {
          name: '充值金额',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          yAxisIndex: 1,
          data: trend.map((item) => Number(item.rechargeAmount ?? 0))
        }
      ]
    },
    true
  )
  chartInstance.resize()
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await loadShopOptions()
  await loadStatistics()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 18px;
  font-weight: bold;
  color: var(--text-color-primary);
}

.header-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-select {
  width: 220px;
}

.filter-date-range {
  width: 280px;
}

.dashboard-content {
  padding: 20px 0;
}

.chart-section {
  margin-top: 8px;
}

.chart-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.trend-chart {
  width: 100%;
  height: 400px;
  min-height: 320px;
}

@media screen and (max-width: 768px) {
  .dashboard-container {
    padding: 10px;
  }

  .filter-select,
  .filter-date-range {
    width: 100%;
  }

  .el-col {
    margin-bottom: 15px;
  }
}
</style>
