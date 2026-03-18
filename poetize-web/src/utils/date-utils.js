/**
 * 日期计算工具函数
 * 从 common.js 拆分，仅 love.vue 使用
 */

/**
 * 计算两个时间相差的年、月、日、时、分、秒
 *
 * https://juejin.cn/post/7153816601156911118
 */
export function timeDiff(oldTime, newTime) {
  oldTime = oldTime.replace(new RegExp('-', 'gm'), '/')
  if (newTime) {
    newTime = newTime.replace(new RegExp('-', 'gm'), '/')
  } else {
    newTime = new Date()
  }

  // 计算比较日期
  const getMaxMinDate = (time, twoTime, type) => {
    let minTime =
      new Date(time).getTime() - new Date(twoTime).getTime() > 0
        ? twoTime
        : time
    let maxTime =
      new Date(time).getTime() - new Date(twoTime).getTime() > 0
        ? time
        : twoTime
    let maxDateDay = new Date(
      new Date(maxTime).getFullYear(),
      new Date(maxTime).getMonth() + 1,
      0
    ).getDate()
    let maxMinDate =
      new Date(minTime).getDate() > maxDateDay
        ? maxDateDay
        : new Date(minTime).getDate()
    let maxMinTong
    if (type === 'month') {
      maxMinTong =
        new Date(maxTime).getFullYear() +
        '/' +
        (new Date(minTime).getMonth() + 1) +
        '/' +
        maxMinDate +
        ' ' +
        new Date(minTime).toLocaleTimeString('chinese', { hour12: false })
    } else {
      maxMinTong =
        new Date(maxTime).getFullYear() +
        '/' +
        (new Date(maxTime).getMonth() + 1) +
        '/' +
        maxMinDate +
        ' ' +
        new Date(minTime).toLocaleTimeString('chinese', { hour12: false })
    }
    return {
      minTime,
      maxTime,
      maxMinTong,
    }
  }

  // 相差年份
  const getYear = (time, twoTime) => {
    let oneYear = new Date(time).getFullYear()
    let twoYear = new Date(twoTime).getFullYear()
    const { minTime, maxTime, maxMinTong } = getMaxMinDate(
      time,
      twoTime,
      'month'
    )
    let chaYear = Math.abs(oneYear - twoYear)
    if (new Date(maxMinTong).getTime() > new Date(maxTime).getTime()) {
      chaYear--
    }
    return chaYear
  }

  // 相差月份
  const getMonth = (time, twoTime, value) => {
    let oneMonth =
      new Date(time).getFullYear() * 12 + (new Date(time).getMonth() + 1)
    let twoMonth =
      new Date(twoTime).getFullYear() * 12 +
      (new Date(twoTime).getMonth() + 1)
    const { minTime, maxTime, maxMinTong } = getMaxMinDate(
      time,
      twoTime,
      'day'
    )
    let chaMonth = Math.abs(oneMonth - twoMonth)
    if (new Date(maxMinTong).getTime() > new Date(maxTime).getTime()) {
      chaMonth--
    }
    if (value) {
      return chaMonth - value
    } else {
      return chaMonth
    }
  }

  // 相差天数
  const getDay = (time, twoTime, value) => {
    let chaTime = Math.abs(
      new Date(time).getTime() - new Date(twoTime).getTime()
    )
    if (value) {
      return parseInt(chaTime / 86400000) - value
    } else {
      return parseInt(chaTime / 86400000)
    }
  }

  // 相差小时
  const getHour = (time, twoTime, value) => {
    let chaTime = Math.abs(
      new Date(time).getTime() - new Date(twoTime).getTime()
    )
    if (value) {
      return parseInt(chaTime / 3600000) - value
    } else {
      return parseInt(chaTime / 3600000)
    }
  }

  // 相差分钟
  const getMinute = (time, twoTime, value) => {
    let chaTime = Math.abs(
      new Date(time).getTime() - new Date(twoTime).getTime()
    )
    if (value) {
      return parseInt(chaTime / 60000) - value
    } else {
      return parseInt(chaTime / 60000)
    }
  }

  // 相差秒
  const getSecond = (time, twoTime, value) => {
    let chaTime = Math.abs(
      new Date(time).getTime() - new Date(twoTime).getTime()
    )
    if (value) {
      return parseInt(chaTime / 1000) - value
    } else {
      return parseInt(chaTime / 1000)
    }
  }

  // 相差年月日时分秒
  const getDiffYMDHMS = (time, twoTime) => {
    const { minTime, maxTime, maxMinTong } = getMaxMinDate(
      time,
      twoTime,
      'day'
    )
    let diffDay1 = getDay(minTime, maxMinTong)
    if (new Date(maxMinTong).getTime() > new Date(maxTime).getTime()) {
      let prevMonth = new Date(maxMinTong).getMonth() - 1
      let lastTime = new Date(maxMinTong).setMonth(prevMonth)
      diffDay1 =
        diffDay1 -
        getDay(
          new Date(lastTime).getFullYear() +
          '/' +
          (new Date(lastTime).getMonth() + 1) +
          '/' +
          new Date(lastTime).getDate(),
          maxMinTong
        )
    }
    let diffYear = getYear(time, twoTime)
    let diffMonth = getMonth(time, twoTime, diffYear * 12)
    let diffDay = getDay(time, twoTime, diffDay1)
    let diffHour = getHour(time, twoTime, getDay(time, twoTime) * 24)
    let diffMinute = getMinute(
      time,
      twoTime,
      getDay(time, twoTime) * 24 * 60 + diffHour * 60
    )
    let diffSecond = getSecond(
      time,
      twoTime,
      getDay(time, twoTime) * 24 * 60 * 60 +
      diffHour * 60 * 60 +
      diffMinute * 60
    )
    return {
      diffYear,
      diffMonth,
      diffDay,
      diffHour,
      diffMinute,
      diffSecond,
    }
  }

  return getDiffYMDHMS(oldTime, newTime)
}

/**
 * 倒计时
 */
export function countdown(time) {
  time = new Date(time.replace(new RegExp('-', 'gm'), '/'))
  let nowTime = new Date()
  //两个时间点的时间差(秒)
  let seconds = parseInt((time.getTime() - nowTime.getTime()) / 1000)
  let d = parseInt(seconds / 3600 / 24)
  let h = parseInt((seconds / 3600) % 24)
  let m = parseInt((seconds / 60) % 60)
  let s = parseInt(seconds % 60)
  return {
    d,
    h,
    m,
    s,
  }
}
