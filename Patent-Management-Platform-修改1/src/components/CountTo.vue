<template>
  <span class="count-to">
    {{ displayValue }}
  </span>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'

const props = defineProps({
  startVal: {
    type: Number,
    default: 0
  },
  endVal: {
    type: Number,
    required: true
  },
  duration: {
    type: Number,
    default: 2000
  },
  autoplay: {
    type: Boolean,
    default: true
  },
  decimals: {
    type: Number,
    default: 0
  },
  decimal: {
    type: String,
    default: '.'
  },
  separator: {
    type: String,
    default: ','
  },
  prefix: {
    type: String,
    default: ''
  },
  suffix: {
    type: String,
    default: ''
  }
})

const localStartVal = ref(props.startVal)
const localDuration = ref(props.duration)
const startTime = ref<number | null>(null)
const timestamp = ref<number | null>(null)
const remaining = ref<number | null>(null)
const rAF = ref<number | null>(null)
const printVal = ref<number | null>(null)

const isCountDown = computed(() => {
  return props.startVal > props.endVal
})

const displayValue = computed(() => {
  return formatNumber(printVal.value || 0)
})

const formatNumber = (num: number) => {
  const numStr = num.toFixed(props.decimals)
  const x = numStr.split('.')
  let x1 = x[0]
  const x2 = x.length > 1 ? props.decimal + x[1] : ''
  const rgx = /(\d+)(\d{3})/
  if (props.separator && typeof props.separator === 'string') {
    while (rgx.test(x1)) {
      x1 = x1.replace(rgx, '$1' + props.separator + '$2')
    }
  }
  return props.prefix + x1 + x2 + props.suffix
}

const easeOutExpo = (t: number, b: number, c: number, d: number) => {
  return c * (-Math.pow(2, -10 * t / d) + 1) * 1024 / 1023 + b
}

const count = (timestampValue: number) => {
  if (!startTime.value) startTime.value = timestampValue
  timestamp.value = timestampValue
  const progress = timestampValue - startTime.value
  remaining.value = localDuration.value - progress

  if (isCountDown.value) {
    printVal.value = localStartVal.value - easeOutExpo(progress, 0, localStartVal.value - props.endVal, localDuration.value)
  } else {
    printVal.value = easeOutExpo(progress, localStartVal.value, props.endVal - localStartVal.value, localDuration.value)
  }

  if (isCountDown.value) {
    printVal.value = printVal.value < props.endVal ? props.endVal : printVal.value
  } else {
    printVal.value = printVal.value > props.endVal ? props.endVal : printVal.value
  }

  if (progress < localDuration.value) {
    rAF.value = requestAnimationFrame(count)
  } else {
    // 动画结束
  }
}

const start = () => {
  localStartVal.value = props.startVal
  startTime.value = null
  localDuration.value = props.duration
  rAF.value = requestAnimationFrame(count)
}

const pause = () => {
  if (rAF.value) {
    cancelAnimationFrame(rAF.value)
    rAF.value = null
  }
}

const resume = () => {
  startTime.value = null
  localDuration.value = remaining.value || props.duration
  localStartVal.value = printVal.value || props.startVal
  rAF.value = requestAnimationFrame(count)
}

const reset = () => {
  startTime.value = null
  cancelAnimationFrame(rAF.value!)
  printVal.value = props.startVal
}

watch(() => props.startVal, () => {
  if (props.autoplay) {
    start()
  }
})

watch(() => props.endVal, () => {
  if (props.autoplay) {
    start()
  }
})

onMounted(() => {
  if (props.autoplay) {
    start()
  }
})
</script>