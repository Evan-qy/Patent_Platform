import { onMounted, onUnmounted } from 'vue'

export function useScrollAnimation(selector = '.animate-on-scroll', activeClass = 'active') {
  let observer: IntersectionObserver | null = null
  let mutationObserver: MutationObserver | null = null
  const observed = new WeakSet<Element>()

  const observeElements = () => {
    if (!observer) return
    const elements = document.querySelectorAll(selector)
    elements.forEach((el) => {
      if (!observed.has(el)) {
        observed.add(el)
        observer?.observe(el)
      }
    })
  }

  onMounted(() => {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add(activeClass)
          }
        })
      },
      {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
      }
    )

    observeElements()

    mutationObserver = new MutationObserver(() => {
      observeElements()
    })

    mutationObserver.observe(document.body, {
      childList: true,
      subtree: true
    })
  })

  onUnmounted(() => {
    observer?.disconnect()
    mutationObserver?.disconnect()
  })
}
