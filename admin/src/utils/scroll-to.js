/**
 * 平滑滚动到指定位置
 */
export function scrollTo(element, to, duration) {
  if (duration <= 0) {
    if (element) {
      element.scrollTop = to
    } else {
      window.scrollTo(0, to)
    }
    return
  }
  
  const difference = to - (element ? element.scrollTop : window.pageYOffset)
  const perTick = difference / duration * 10
  
  setTimeout(() => {
    const currentTop = element ? element.scrollTop : window.pageYOffset
    const newTop = currentTop + perTick
    
    if (element) {
      element.scrollTop = newTop
    } else {
      window.scrollTo(0, newTop)
    }
    
    if (Math.abs(newTop - to) < Math.abs(perTick)) {
      if (element) {
        element.scrollTop = to
      } else {
        window.scrollTo(0, to)
      }
    } else {
      scrollTo(element, to, duration - 10)
    }
  }, 10)
}
