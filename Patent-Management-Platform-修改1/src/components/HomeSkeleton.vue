<template>
  <div class="home-container">
    <!-- 导航栏骨架屏 -->
    <div class="home-header skeleton">
      <div class="container">
        <div class="header-content">
          <div class="logo skeleton-block"></div>
          <div class="nav-menu skeleton-block"></div>
          <div class="header-actions skeleton-block"></div>
        </div>
      </div>
    </div>

    <!-- 主内容骨架屏 -->
    <main class="home-main">
      <div class="home-banner skeleton-block"></div>
      <div class="home-features">
        <div class="section-title skeleton-block"></div>
        <div class="features-grid">
          <div class="feature-card skeleton" v-for="i in 4" :key="i">
            <div class="feature-icon skeleton-block"></div>
            <div class="feature-title skeleton-block"></div>
            <div class="feature-description skeleton-block"></div>
          </div>
        </div>
      </div>
      <div class="home-advantages skeleton-block"></div>
      <div class="home-cases">
        <div class="section-title skeleton-block"></div>
        <div class="cases-grid">
          <div class="case-card skeleton" v-for="i in 3" :key="i"></div>
        </div>
      </div>
      <div class="home-cta skeleton-block"></div>
    </main>

    <!-- 页脚骨架屏 -->
    <div class="home-footer skeleton-block"></div>
  </div>
</template>

<style scoped>
.skeleton {
  background: #f2f2f2;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.skeleton::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: skeleton-loading 1.5s infinite;
}

@keyframes skeleton-loading {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.skeleton-block {
  height: 20px;
  background: #f2f2f2;
  border-radius: 4px;
  margin-bottom: 16px;
}

/* 适配首页样式 */
.home-header .skeleton-block {
  height: 40px;
  width: 120px;
  margin-right: 20px;
}

.home-banner .skeleton-block {
  height: 200px;
  width: 100%;
}

.feature-card .skeleton-block {
  margin: 0 auto 16px;
}

.feature-icon .skeleton-block {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.feature-title .skeleton-block {
  width: 80%;
  height: 24px;
}

.feature-description .skeleton-block {
  width: 100%;
  height: 60px;
}

.case-card.skeleton {
  height: 200px;
  width: 100%;
  margin-bottom: 24px;
}

.home-advantages .skeleton-block {
  height: 300px;
  width: 100%;
}

.home-cta .skeleton-block {
  height: 150px;
  width: 100%;
}

.home-footer .skeleton-block {
  height: 200px;
  width: 100%;
}
</style>