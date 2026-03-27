<template>
  <div class="friend-wrap">
    <div class="friend-box" v-if="!$common.isEmpty($store.state.currentUser)">
      <div class="friend-aside">
        <div>
          <!-- 头像 -->
          <div>
            <el-avatar fit="cover" @click="changeAvatar(1)" class="user-thumb"
                      :src="$common.getAvatarUrl($store.state.currentUser.avatar)"
                      :fallback-src="$common.getDefaultAvatar()"/>
          </div>
          <!-- 聊天 -->
          <div id="chat" class="friend-chat aside-active" @click="isActive($event, 'aside-active', 1)">
            <div>
              <svg viewBox="0 0 1024 1024" width="24" height="24">
                <path
                  d="M355.76 85.68A175.808 175.808 0 0 1 496 16c67.616 0 126.368 38.208 155.84 94.192a31.712 31.712 0 0 0 47.872 9.744C731.712 94.736 772.144 80 816 80c105.968 0 192 86.032 192 192s-86.032 192-192 192c-93.36 0-171.232-66.768-188.448-155.12a175.632 175.632 0 0 1-140.144 58.912C459.44 469.328 366.368 544 256 544 123.536 544 16 436.464 16 304S123.536 64 256 64c35.584 0 69.376 7.76 99.76 21.68z"
                  fill="#CBE9EA"></path>
                <path
                  d="M16 400.8A128.864 128.864 0 0 1 144.8 272h734.4A128.864 128.864 0 0 1 1008 400.8v305.6a128.864 128.864 0 0 1-128.8 128.8h-161.6L809.6 1008 568.8 835.2H144.8A128.864 128.864 0 0 1 16 706.4v-305.6z"
                  fill="#FFD140"></path>
                <path
                  d="M144.8 835.2c-21.408 0 459.952-30.704 678.288-44.56A112 112 0 0 0 928 678.864V412.064a144 144 0 0 0-42.688-102.32L847.2 272C918.288 272 1008 329.712 1008 400.8v305.6a128.864 128.864 0 0 1-128.8 128.8h-161.6L809.6 1008 568.8 835.2H144.8z"
                  fill="#D9B236"></path>
                <path d="M288 552m-56 0a56 56 0 1 0 112 0 56 56 0 1 0-112 0Z" fill="#FFFFFF"></path>
                <path d="M288 552m-56 0a56 56 0 1 0 112 0 56 56 0 1 0-112 0Z" fill="#FFFFFF"></path>
                <path d="M512 552m-56 0a56 56 0 1 0 112 0 56 56 0 1 0-112 0Z" fill="#FFFFFF"></path>
                <path d="M736 552m-56 0a56 56 0 1 0 112 0 56 56 0 1 0-112 0Z" fill="#FFFFFF"></path>
                <path
                  d="M585.616 225.312A120.064 120.064 0 0 1 472 384a119.408 119.408 0 0 1-74.272-25.776A144.048 144.048 0 0 1 272 432c-79.472 0-144-64.528-144-144 0-26.224 7.024-50.832 19.296-72.016"
                  fill="#CBE9EA"></path>
                <path
                  d="M585.616 225.312c3.264-12.4 6.384 25.152 6.384 38.688 0 66.224-53.776 120-120 120a119.408 119.408 0 0 1-74.272-25.776A144.048 144.048 0 0 1 272 432c-79.472 0-144-64.528-144-144 0-26.224 17.84-96.464 19.296-72.016 10.784 179.808 167.712 183.392 250.432 94.992 67.824 66.656 166.24-3.392 187.888-85.664z"
                  fill="#ADC6C7"></path>
              </svg>
            </div>
            <div class="friend-text">聊天</div>
          </div>
          <!-- 好友 -->
          <div class="friend-chat" @click="isActive($event, 'aside-active', 2)">
            <div>
              <svg viewBox="0 0 1024 1024" width="24" height="24">
                <path
                  d="M560.6 476.7c-76 0-137.9-61.9-137.9-137.9s61.9-137.9 137.9-137.9 137.9 61.9 137.9 137.9-61.8 137.9-137.9 137.9zM800 715.2"
                  fill="#62A8EA"></path>
                <path
                  d="M801.6 715.5c9.2 0 16.6-7.5 16.6-16.6 0-34.5-6.8-68-20.2-99.6-12.9-30.5-31.3-57.9-54.9-81.3-23.5-23.5-50.9-42-81.3-54.9-31.6-13.4-65.1-20.2-99.6-20.2a16 16 0 0 0-3.9 0.5c-33.7 0.3-66.4 7.1-97.2 20.1-30.5 12.9-57.9 31.4-81.3 54.9-23.5 23.5-42 50.9-54.9 81.3-13.4 31.6-20.2 65.1-20.2 99.6 0 9.2 7.4 16.6 16.6 16.6 9.2 0.1 471.1-0.4 480.3-0.4z"
                  fill="#62A8EA"></path>
                <path
                  d="M490.6 476.7c-76 0-137.9-61.9-137.9-137.9s61.9-137.9 137.9-137.9 137.9 61.9 137.9 137.9-61.8 137.9-137.9 137.9z m0-242.6c-57.7 0-104.6 47-104.6 104.6 0 57.7 46.9 104.7 104.6 104.7 57.7 0 104.6-47 104.6-104.6 0.1-57.7-46.8-104.7-104.6-104.7zM295.7 467.2c-2 0-4-0.3-6-1.1-23.5-9.1-43.7-24.9-58.2-45.7-14.9-21.3-22.8-46.4-22.8-72.4 0-69.8 56.8-126.6 126.6-126.6 9.2 0 16.6 7.5 16.6 16.6 0 9.2-7.4 16.6-16.6 16.6-51.5 0-93.3 41.8-93.3 93.3 0 38.3 24 73.3 59.7 87.1 8.6 3.3 12.8 13 9.5 21.5-2.5 6.7-8.9 10.7-15.5 10.7z m434.3 248c-9.2 0-16.6-7.4-16.6-16.6 0-71-32.7-136.2-89.8-178.7-7.4-5.5-8.9-15.9-3.4-23.3s15.9-8.9 23.3-3.4c31.3 23.3 57.3 54 75 88.7 18.7 36.3 28.1 75.6 28.1 116.7 0 9.1-7.5 16.6-16.6 16.6z m0 0"
                  fill="#333333"></path>
                <path
                  d="M171.2 645.9c-0.7 0-1.4-0.1-2.1-0.1-9.1-1.1-15.6-9.4-14.4-18.6 5.1-40.8 19.3-78.6 42.2-112.4 22-32.2 51.4-59.4 85.4-78.8 8-4.5 18.2-1.7 22.7 6.3s1.7 18.2-6.3 22.7c-61.9 35.2-102.4 95.8-111.1 166.3-0.9 8.5-8 14.6-16.4 14.6z m80.1 70.1c-9.2 0-16.6-7.5-16.6-16.6 0-34.5 6.8-68 20.2-99.6 12.9-30.5 31.3-57.9 54.9-81.3 23.5-23.5 50.9-42 81.3-54.9 31.6-13.4 65.1-20.2 99.6-20.2 9.2 0 16.6 7.4 16.6 16.6 0 9.2-7.5 16.6-16.6 16.6-122.8 0-222.7 99.9-222.7 222.7 0 9.2-7.5 16.7-16.7 16.7z m0 0"
                  fill="#333333"></path>
                <path
                  d="M769.3 218.3h-40c-2.6 0-4.7-2.1-4.7-4.7s2.1-4.7 4.7-4.7h40c2.6 0 4.7 2.1 4.7 4.7 0.1 2.6-2.1 4.7-4.7 4.7z"
                  fill="#62A8EA"></path>
                <path
                  d="M749.3 237.3c-2.9 0-5.2-2.3-5.2-5.2v-36.9c0-2.9 2.3-5.2 5.2-5.2 2.9 0 5.2 2.3 5.2 5.2v36.9c0 2.8-2.3 5.2-5.2 5.2z"
                  fill="#62A8EA"></path>
              </svg>
            </div>
            <div class="friend-text">好友</div>
          </div>
          <!-- 群聊 -->
          <div class="friend-chat" @click="isActive($event, 'aside-active', 3)">
            <div>
              <svg viewBox="0 0 1024 1024" width="24" height="24">
                <path
                  d="M966.08 738.12l-54.26-182.22c16.74-37.82 25.24-77.78 25.24-118.82 0-177.22-160.46-321.38-357.68-321.38s-357.68 144.16-357.68 321.38c0 2.26 0.02 4.58 0.08 6.92C137.28 486.36 80 567.54 80 660.56c0 31.38 6.42 61.94 19.12 90.88l-41.2 138.3a9.94 9.94 0 0 0 2.26 9.66 10.006 10.006 0 0 0 9.48 2.96l140.96-31.02c43.42 24.2 93.42 36.98 144.84 36.96 113.74 0 211.6-62.32 253.6-150.98 56.54-4.18 111.48-20.5 159.52-47.46l185.76 40.88c3.46 0.76 7.06-0.36 9.48-2.96a10.036 10.036 0 0 0 2.26-9.66z m-610.62 150.18c-49.28 0.02-97.08-12.52-138.26-36.22-1.52-0.88-3.24-1.32-4.98-1.32-0.72 0-1.44 0.08-2.16 0.22l-128.12 28.2 37.4-125.58c0.68-2.32 0.52-4.82-0.5-7.02-12.5-27.34-18.84-56.28-18.84-86.02 0-125.6 114.6-227.76 255.46-227.76 140.88 0 255.48 102.16 255.48 227.76 0 125.58-114.6 227.74-255.48 227.74z m413.68-198.8a9.992 9.992 0 0 0-7.14 1.1c-43.4 24.98-93 40.68-144.32 45.88 8.6-23.94 13.26-49.44 13.26-75.92 0-136.62-123.58-247.76-275.48-247.76-40.52 0-79.04 7.92-113.74 22.12 1.3-165.2 152.26-299.22 337.66-299.22 186.2 0 337.68 135.2 337.68 301.38 0 39.4-8.4 77.74-24.98 113.96a10.12 10.12 0 0 0-0.5 7.02l50.48 169.5-172.92-38.06z"
                  fill="#23130D"></path>
                <path
                  d="M355.46 432.8C214.6 432.8 100 534.96 100 660.56c0 29.74 6.34 58.68 18.84 86.02 1.02 2.2 1.18 4.7 0.5 7.02l-37.4 125.58 128.12-28.2c0.72-0.14 1.44-0.22 2.16-0.22 1.74 0 3.46 0.44 4.98 1.32 41.18 23.7 88.98 36.24 138.26 36.22 140.88 0 255.48-102.16 255.48-227.74 0-125.6-114.6-227.76-255.48-227.76z m-82.96 96.28h143.44c5.52 0 10 4.46 10 10 0 5.52-4.48 10-10 10h-143.44c-5.52 0-10-4.48-10-10 0-5.54 4.48-10 10-10zM498.18 784.8H190.26c-5.52 0-10-4.46-10-10 0-5.52 4.48-10 10-10h307.92c5.52 0 10 4.48 10 10 0 5.54-4.48 10-10 10z m0-58.92H190.26c-5.52 0-10-4.48-10-10s4.48-10 10-10h307.92c5.52 0 10 4.48 10 10s-4.48 10-10 10z m0-58.94H190.26c-5.52 0-10-4.48-10-10s4.48-10 10-10h307.92c5.52 0 10 4.48 10 10s-4.48 10-10 10z m0-58.94H190.26c-5.52 0-10-4.48-10-10s4.48-10 10-10h307.92c5.52 0 10 4.48 10 10s-4.48 10-10 10z"
                  fill="#EC7063"></path>
                <path
                  d="M942.06 727.56l-172.92-38.06a9.992 9.992 0 0 0-7.14 1.1c-43.4 24.98-93 40.68-144.32 45.88 8.6-23.94 13.26-49.44 13.26-75.92 0-136.62-123.58-247.76-275.48-247.76-40.52 0-79.04 7.92-113.74 22.12 1.3-165.2 152.26-299.22 337.66-299.22 186.2 0 337.68 135.2 337.68 301.38 0 39.4-8.4 77.74-24.98 113.96a10.12 10.12 0 0 0-0.5 7.02l50.48 169.5z"
                  fill="#F9C139"></path>
                <path
                  d="M942.06 727.56l-172.92-38.06a9.992 9.992 0 0 0-7.14 1.1c-43.4 24.98-93 40.68-144.32 45.88 8.6-23.94 13.26-49.44 13.26-75.92 0-136.62-123.58-247.76-275.48-247.76-40.52 0-79.04 7.92-113.74 22.12 0.66-82.68 38.78-157.56 99.98-211.72-44.9 50.8-71.92 114.74-72.46 184.2 34.7-14.2 73.22-22.12 113.74-22.12 151.9 0 275.48 111.14 275.48 247.76 0 26.48-4.66 51.98-13.26 75.92 51.32-5.2 100.92-20.9 144.32-45.88 2.16-1.24 4.7-1.64 7.14-1.1l134.7 29.64 10.7 35.94z"
                  fill="#23130D" opacity=".2"></path>
                <path
                  d="M425.94 539.08c0 5.52-4.48 10-10 10h-143.44c-5.52 0-10-4.48-10-10 0-5.54 4.48-10 10-10h143.44c5.52 0 10 4.46 10 10zM508.18 598c0 5.52-4.48 10-10 10H190.26c-5.52 0-10-4.48-10-10s4.48-10 10-10h307.92c5.52 0 10 4.48 10 10zM508.18 656.94c0 5.52-4.48 10-10 10H190.26c-5.52 0-10-4.48-10-10s4.48-10 10-10h307.92c5.52 0 10 4.48 10 10zM508.18 715.88c0 5.52-4.48 10-10 10H190.26c-5.52 0-10-4.48-10-10s4.48-10 10-10h307.92c5.52 0 10 4.48 10 10zM508.18 774.8c0 5.54-4.48 10-10 10H190.26c-5.52 0-10-4.46-10-10 0-5.52 4.48-10 10-10h307.92c5.52 0 10 4.48 10 10z"
                  fill="#FFFFFF"></path>
              </svg>
            </div>
            <div class="friend-text">群聊</div>
          </div>
        </div>
        <div class="friend-set">
          <!-- 朋友圈 -->
          <div @click.stop="openFriendCircle($store.state.currentUser.id, $store.state.currentUser.avatar)"
               style="margin-bottom: 15px">
            <svg viewBox="0 0 1024 1024" width="24" height="24">
              <path
                d="M512 1024C229.230021 1024 0 794.769979 0 512S229.230021 0 512 0s512 229.230021 512 512-229.230021 512-512 512z m0.42935-800.000537a279.249308 279.249308 0 0 0-32.561912 1.915975c-26.152788 2.948562-51.221468 9.458583-74.787421 18.912872 0 0 194.316344 191.608218 199.490012 198.731137V239.228512c-5.79086-1.964277-11.707304-3.733199-17.67205-5.379757-23.735547-6.361895-48.707623-9.848218-74.467556-9.848218z m202.969895 83.240252c-7.789484-7.760503-15.950356-14.934943-24.378499-21.587723a286.849878 286.849878 0 0 0-66.285216-39.300562s1.429736 272.315304 0 281.01179l145.044126-144.496704a293.183866 293.183866 0 0 0-8.728687-16.21226c-12.300881-21.220629-27.433325-41.288453-45.65065-59.413468z m63.669401 98.298633S586.82499 599.134457 579.626935 604.290952h205.088738c1.970717-5.770465 3.748226-11.667589 5.373317-17.609795 6.40805-23.703346 9.911547-48.510122 9.911547-74.202432 0-10.979556-0.691254-21.810985-1.951397-32.445987a282.689476 282.689476 0 0 0-18.980494-74.49439zM493.867472 625.806759l145.040905 144.497778a287.548646 287.548646 0 0 0 16.293837-8.69434c21.298985-12.20857 41.414038-27.312034 59.65605-45.487497 7.761577-7.739036 14.938164-15.893468 21.642466-24.290482 16.341065-20.537962 29.531774-42.764344 39.439027-66.050147 0 0-273.31891 1.427589-282.072285 0.024688z m-74.417107-45.38982v204.353476a294.790709 294.790709 0 0 0 17.673123 5.356143c23.764528 6.386583 48.686155 9.872906 74.470776 9.872905 10.991363 0 21.862507-0.68696 32.512537-1.914901a286.830558 286.830558 0 0 0 74.788495-18.938633c0.023614 0-194.270189-191.555623-199.444931-198.72899z m-20.411304-85.230289L254.046457 639.682281c2.713493 5.527883 5.647027 10.932327 8.727614 16.235874 12.252579 21.271078 27.409711 41.31314 45.651724 59.46499 7.764797 7.76265 15.950356 14.909182 24.378499 21.566255 20.630273 16.335698 42.892075 29.425509 66.309903 39.297342 0 0-1.479111-272.339992-0.07621-281.060092z m-165.155958-57.744369c-6.384436 23.678658-9.883639 48.511195-9.88364 74.203505 0 10.952721 0.691254 21.808839 1.947103 32.4213 2.959296 26.083019 9.441409 51.063681 19.006256 74.495463 0 0 192.223262-193.597182 199.420243-198.731136H239.258566c-1.97179 5.745778-3.74608 11.665442-5.375463 17.610868z m133.531102-173.947975c-21.346214 12.232184-41.461266 27.338868-59.654976 45.463883-7.837786 7.787338-14.986465 15.941769-21.66608 24.290482-16.368973 20.537962-29.531774 42.789031-39.463715 66.098448 0 0 273.367212-1.449057 282.095899-0.049375L383.705895 254.80104c-5.519296 2.748914-10.967748 5.624486-16.29169 8.694339z"
                fill="#07C160"></path>
            </svg>
          </div>
          <!-- 暗色模式切换 -->
          <div @click.stop="toggleDarkMode()" style="margin-bottom: 15px" :title="isDarkMode ? '切换到亮色模式' : '切换到暗色模式'">
            <svg v-if="!isDarkMode" viewBox="0 0 1024 1024" width="24" height="24">
              <!-- 月亮图标（暗色模式关闭时显示） -->
              <path d="M593.664 102.4a409.6 409.6 0 1 0 327.936 327.936A409.6 409.6 0 0 1 593.664 102.4z" fill="#FFD700"></path>
            </svg>
            <svg v-else viewBox="0 0 1024 1024" width="24" height="24">
              <!-- 太阳图标（暗色模式开启时显示） -->
              <path d="M512 256a256 256 0 1 0 256 256 256 256 0 0 0-256-256zm0 448a192 192 0 1 1 192-192 192 192 0 0 1-192 192z" fill="#FFD700"></path>
              <path d="M512 128a32 32 0 0 1-32-32V32a32 32 0 0 1 64 0v64a32 32 0 0 1-32 32zm0 896a32 32 0 0 1-32-32v-64a32 32 0 0 1 64 0v64a32 32 0 0 1-32 32zM224 256a32 32 0 0 1-22.624-54.624l45.248-45.248a32 32 0 0 1 45.248 45.248l-45.248 45.248A31.808 31.808 0 0 1 224 256zm576 576a32 32 0 0 1-22.624-54.624l45.248-45.248a32 32 0 0 1 45.248 45.248l-45.248 45.248A31.808 31.808 0 0 1 800 832zM96 544H32a32 32 0 0 1 0-64h64a32 32 0 0 1 0 64zm896 0h-64a32 32 0 0 1 0-64h64a32 32 0 0 1 0 64zM269.248 832a31.808 31.808 0 0 1-22.624-9.376l-45.248-45.248a32 32 0 0 1 45.248-45.248l45.248 45.248A32 32 0 0 1 269.248 832zM800 256a31.808 31.808 0 0 1-22.624-9.376l-45.248-45.248a32 32 0 0 1 45.248-45.248l45.248 45.248A32 32 0 0 1 800 256z" fill="#FFD700"></path>
            </svg>
          </div>
          <!-- 展开/收起 -->
          <div @click.stop="changeAside()" :title="showBodyLeft ? '收起左侧面板' : '展开左侧面板'">
            <!-- 展开图标（当左侧面板隐藏时显示） -->
            <svg v-if="!showBodyLeft" viewBox="0 0 1024 1024" width="24" height="24">
              <path d="M798.814815 609.185185c-5.499259 0-10.903704-2.085926-15.075556-6.257778-8.343704-8.343704-8.343704-21.807407 0-30.151111l60.776297-60.776296-60.776297-60.776296c-8.343704-8.343704-8.343704-21.807407 0-30.151111s21.807407-8.343704 30.151111 0l75.851852 75.851851c8.343704 8.343704 8.343704 21.807407 0 30.151112l-0.094815 0.094814-75.757037 75.757037c-4.171852 4.171852-9.576296 6.257778-15.075555 6.257778zM874.666667 248.888889h-725.333334c-11.757037 0-21.333333-9.576296-21.333333-21.333333s9.576296-21.333333 21.333333-21.333334h725.333334c11.757037 0 21.333333 9.576296 21.333333 21.333334s-9.576296 21.333333-21.333333 21.333333zM642.37037 533.333333h-493.037037c-11.757037 0-21.333333-9.576296-21.333333-21.333333s9.576296-21.333333 21.333333-21.333333h493.037037c11.757037 0 21.333333 9.576296 21.333334 21.333333s-9.576296 21.333333-21.333334 21.333333zM874.666667 817.777778h-725.333334c-11.757037 0-21.333333-9.576296-21.333333-21.333334s9.576296-21.333333 21.333333-21.333333h725.333334c11.757037 0 21.333333 9.576296 21.333333 21.333333s-9.576296 21.333333-21.333333 21.333334z" fill="currentColor"></path>
            </svg>
            <!-- 收起图标（当左侧面板显示时显示） -->
            <svg v-else viewBox="0 0 1024 1024" width="24" height="24">
              <path d="M874.666667 249.362963h-725.333334c-11.757037 0-21.333333-9.576296-21.333333-21.333333s9.576296-21.333333 21.333333-21.333334h725.333334c11.757037 0 21.333333 9.576296 21.333333 21.333334S886.423704 249.362963 874.666667 249.362963zM225.185185 609.659259c-5.499259 0-10.903704-2.085926-15.075555-6.257778l-75.851852-75.851851c-8.343704-8.343704-8.343704-21.807407 0-30.151111l0.094815-0.094815 75.757037-75.757037c8.343704-8.343704 21.807407-8.343704 30.151111 0s8.343704 21.807407 0 30.151111l-60.776297 60.776296 60.776297 60.776296c8.343704 8.343704 8.343704 21.807407 0 30.151111-4.171852 4.171852-9.576296 6.257778-15.075556 6.257778zM874.666667 533.807407h-493.037037c-11.757037 0-21.333333-9.576296-21.333334-21.333333s9.576296-21.333333 21.333334-21.333333h493.037037c11.757037 0 21.333333 9.576296 21.333333 21.333333S886.423704 533.807407 874.666667 533.807407z" fill="currentColor"></path>
              <path d="M874.666667 818.251852h-725.333334c-11.757037 0-21.333333-9.576296-21.333333-21.333333s9.576296-21.333333 21.333333-21.333334h725.333334c11.757037 0 21.333333 9.576296 21.333333 21.333334S886.423704 818.251852 874.666667 818.251852z" fill="currentColor"></path>
            </svg>
          </div>
        </div>
      </div>
      <div class="friend-bode" id="body-group">
        <div class="body-left" v-show="showBodyLeft">
          <!-- 搜索 -->
          <div>
            <el-input v-model="showFriendValue" round placeholder="搜索" class="im-input"></el-input>
          </div>
          <!-- 聊天 -->
          <div class="aside-list" v-show="type === 1">
            <!-- 系统消息 -->
            <div class="im-user im-active system-message" @click="isActive($event, 'im-active', null, 1)">
              <div>
                <el-badge :value="$store.state.systemMessageBadge" :show-zero="false" :max="99">
                  <svg viewBox="0 0 1024 1024" width="40" height="40">
                    <path
                      d="M462.7968 127.2832L338.0736 251.2896a69.72416 69.72416 0 0 1-49.2032 20.2752H173.1072c-59.2896 0-107.3152 48.0256-107.3152 107.3152v264.6528c0 59.2896 48.0256 107.3152 107.3152 107.3152h121.7536c19.2 0 37.5296 7.8848 50.7392 21.8624l115.712 122.4192c43.3664 45.8752 120.4736 15.2064 120.4736-47.9232V176.7936c0-62.0544-74.9568-93.2352-118.9888-49.5104z"
                      fill="#8C7BFD"></path>
                    <path
                      d="M682.3424 732.672c-15.9744 0-31.6928-7.424-41.6768-21.4016-16.4352-22.9888-11.1616-54.9888 11.8272-71.424 40.6528-29.1328 64.9216-76.288 64.9216-126.2592 0-51.6096-25.6-99.6864-68.4032-128.6656-23.4496-15.8208-29.5936-47.6672-13.7728-71.0656 15.8208-23.4496 47.6672-29.5936 71.0656-13.7728 71.0656 48.0256 113.5104 127.8464 113.5104 213.504 0 82.8928-40.2944 161.1776-107.7248 209.5104a51.03616 51.03616 0 0 1-29.7472 9.5744z"
                      fill="#FFE37B"></path>
                    <path
                      d="M792.4736 838.7072c-13.824 0-27.648-5.5808-37.7344-16.5888-19.0976-20.8384-17.7152-53.248 3.1232-72.3456 67.072-61.5424 105.5232-149.0432 105.5232-240.0256 0-90.1632-35.9936-174.0288-101.376-236.1344-20.48-19.456-21.3504-51.8656-1.8432-72.3968 19.456-20.48 51.8656-21.3504 72.3968-1.8432 85.9648 81.664 133.2736 191.8976 133.2736 310.3744 0 119.6032-50.5344 234.5472-138.7008 315.4432a51.06176 51.06176 0 0 1-34.6624 13.5168z"
                      fill="#FFE37B"></path>
                  </svg>
                </el-badge>
              </div>
              <div class="im-user-right">
                <div>系统消息</div>
              </div>
            </div>
            <div style="overflow-y: auto;height: calc(100% - 70px)">
              <!-- 群聊天 -->
              <div class="im-user im-group-current"
                   v-for="(item, index) in groupChats"
                   :key="index"
                   v-show="groups[item] && groups[item].groupName && (groups[item].groupName.includes(showFriendValue) || $common.isEmpty(showFriendValue))"
                   @click="handleGroupChatClick($event, item)"
                   @touchstart="handleTouchStart($event, item)"
                   @touchend="handleTouchEnd($event, item)"
                   @contextmenu="handleContextMenu($event, item)">
                <div>
                  <el-badge :value="(groupMessageBadge || {})[item] || 0" :show-zero="false" :max="99">
                    <el-avatar fit="cover"
                              lazy
                              :size="50"
                              :src="$common.getAvatarUrl(groups[item] ? groups[item].avatar : '')"
                              :fallback-src="$common.getDefaultAvatar()"/>
                  </el-badge>
                </div>
                <div class="im-user-right">
                  <div class="im-user-header">
                    <span class="im-user-name">{{groups[item] ? groups[item].groupName : '未知群聊'}}</span>
                    <span class="im-message-time" v-if="lastMessagePreviews[`group_${item}`] && lastMessagePreviews[`group_${item}`].createTime">
                      {{formatTimeWithTick(lastMessagePreviews[`group_${item}`].createTime, minuteTick)}}
                    </span>
                  </div>
                  <div class="im-down">
                    <template v-if="lastMessagePreviews[`group_${item}`]">
                      <span v-html="getMessagePreview(lastMessagePreviews[`group_${item}`].content)"></span>
                    </template>
                    <template v-else-if="groupMessages[item] && groupMessages[item].length > 0">
                      <span v-html="getMessagePreview(groupMessages[item][groupMessages[item].length-1].content)"></span>
                    </template>
                  </div>
                </div>
              </div>
              <!-- 聊天 -->
              <div class="im-user im-user-current"
                   v-for="(item, index) in imChats"
                   :key="index"
                   v-show="friends[item].remark.includes(showFriendValue) || $common.isEmpty(showFriendValue)"
                   @click="isActive($event, 'im-active', null, 2, item, 2)">
                <div>
                  <el-badge :value="(imMessageBadge || {})[item] || 0" :show-zero="false" :max="99">
                    <el-avatar fit="cover"
                              lazy
                              :size="40"
                              :src="$common.getAvatarUrl(friends[item].avatar)"
                              :fallback-src="$common.getDefaultAvatar()"/>
                  </el-badge>
                </div>
                <div class="im-user-right">
                  <div class="im-user-header">
                    <span class="im-user-name">{{friends[item].remark}}</span>
                    <span class="im-message-time" v-if="lastMessagePreviews[`friend_${item}`] && lastMessagePreviews[`friend_${item}`].createTime">
                      {{formatTimeWithTick(lastMessagePreviews[`friend_${item}`].createTime, minuteTick)}}
                    </span>
                  </div>
                  <div class="im-down">
                    <template v-if="lastMessagePreviews[`friend_${item}`]">
                      <span v-html="getMessagePreview(lastMessagePreviews[`friend_${item}`].content)"></span>
                    </template>
                    <template v-else-if="imMessages[item] && imMessages[item].length > 0">
                      <span v-html="getMessagePreview(imMessages[item][imMessages[item].length-1].content)"></span>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <!-- 好友 -->
          <div class="aside-list" v-show="type === 2">
            <!-- 添加好友 -->
            <div class="im-user friend-active" @click="isActive($event, 'friend-active', null, 3)">
              <div>
                <svg viewBox="0 0 1024 1024" width="40" height="40">
                  <path d="M0 0m512 0l0 0q512 0 512 512l0 0q0 512-512 512l0 0q-512 0-512-512l0 0q0-512 512-512Z"
                        fill="#FF8B38"></path>
                  <path
                    d="M503.808 251.4688c88.7808 0 160.7552 71.9616 160.7552 160.7552 0 88.7808-71.9744 160.7552-160.768 160.7552-88.7808 0-160.7424-71.9744-160.7424-160.768 0-88.7808 71.9616-160.7424 160.7552-160.7424z m0 38.4a122.3552 122.3552 0 1 0 0 244.7104 122.3552 122.3552 0 0 0 0-244.7104zM377.9584 570.88l29.312 24.8064-90.4192 106.8416a19.2 19.2 0 0 0 12.7232 31.5136l1.9328 0.0896h204.992v38.4H331.52a57.6 57.6 0 0 1-45.824-92.4928l1.856-2.304 90.4192-106.8544zM750.08 651.3536v38.4H573.5168v-38.4z"
                    fill="#FFFFFF"></path>
                  <path d="M680.9984 587.776v165.5552h-38.4V587.776z" fill="#FFFFFF"></path>
                </svg>
              </div>
              <div class="im-friend">
                好友请求
              </div>
            </div>
            <div style="margin: 0 30px 0 30px">
              <el-divider dashed>
                {{Object.keys(friends).length}}位联系人
              </el-divider>
            </div>
            <!-- 好友列表 -->
            <div style="overflow-y: auto;height: calc(100% - 140px)">
              <div class="im-user"
                   v-for="(item, index) in Object.values(friends).reverse()"
                   :key="index"
                   v-show="item.remark.includes(showFriendValue) || $common.isEmpty(showFriendValue)"
                   @click="isActive($event, 'friend-active', null, 4, item)">
                <div>
                  <el-avatar fit="cover"
                            lazy
                            style="cursor: pointer"
                            @click.stop="openFriendCircle(item.friendId, item.avatar)"
                            :size="40"
                            :src="$common.getAvatarUrl(item.avatar)"
                            :fallback-src="$common.getDefaultAvatar()"/>
                </div>
                <div class="im-friend">
                  {{item.remark}}
                </div>
              </div>
            </div>
          </div>
          <!-- 群聊 -->
          <div class="aside-list" v-show="type === 3">
            <div style="margin: 0 30px 0 30px">
              <el-divider dashed>
                {{Object.keys(groups).length}}个群聊
              </el-divider>
            </div>
            <div class="im-user-group"
                 v-for="(item, index) in Object.values(groups).reverse()"
                 :key="index"
                 v-show="item && item.groupName && (item.groupName.includes(showFriendValue) || $common.isEmpty(showFriendValue))"
                 @click="isActive($event, 'im-group', null, 5, item)">
              <div>
                <el-avatar fit="cover"
                          :size="50"
                          lazy
                          :src="$common.getAvatarUrl(item.avatar)"
                          :fallback-src="$common.getDefaultAvatar()"/>
              </div>
              <div class="im-user-right">
                <div>{{item.groupName}}</div>
                <div class="im-down">
                  <span class="group-tag" v-if="item.masterFlag || item.adminFlag">
                    <template v-if="item.masterFlag">
                      群主
                    </template>
                    <template v-else-if="item.adminFlag">
                      管理员
                    </template>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="body-right" v-if="subType === 1">
          <div style="height: 60px;background-color: var(--maxWhite);display: flex;align-items: center;">
            <!-- 移动端返回按钮 -->
            <div v-if="isMobile() && !showBodyLeft" class="mobile-back-btn" @click="showChatList()">
              <el-icon><ArrowLeft /></el-icon>
            </div>
            <span style="line-height: 60px;margin-left: 20px;font-size: 18px">系统消息</span>
          </div>
          <div style="background: var(--midWhite);overflow-y: auto;height: calc(100% - 60px);">
            <template v-if="!$common.isEmpty(systemMessages)">
              <div class="msg-one"
                   v-for="(item, index) in systemMessages"
                   :key="index">
                <!-- 图标 -->
                <div>
                  <svg viewBox="0 0 1024 1024" width="50" height="50">
                    <path
                      d="M752.768 955.392H257.92c-112.384 0-203.392-91.008-203.392-203.392V257.152C54.528 144.768 145.536 53.76 257.92 53.76h494.848C865.152 53.76 956.16 144.768 956.16 257.152v494.848c0 112.384-91.008 203.392-203.392 203.392z"
                      fill="#FF4D3C"></path>
                    <path
                      d="M616.576 765.952H370.56c-67.968 0-123.008-55.04-123.008-123.008V396.8c0-67.968 55.04-123.008 123.008-123.008h166.272c9.984 0 18.176 8.064 18.176 18.176 0 9.984-8.064 18.176-18.176 18.176H370.56c-47.872 0-86.656 38.784-86.656 86.656v246.016c0 47.872 38.784 86.656 86.656 86.656h246.016c47.872 0 86.656-38.784 86.656-86.656V469.12c0-9.984 8.064-18.176 18.176-18.176 9.984 0 18.176 8.064 18.176 18.176v173.824c0 67.968-55.04 123.008-123.008 123.008z"
                      fill="#FFFFFF"></path>
                    <path d="M694.784 312.704m-113.408 0a113.408 113.408 0 1 0 226.816 0 113.408 113.408 0 1 0-226.816 0Z"
                          fill="#FFFFFF"></path>
                  </svg>
                </div>
                <!-- 内容 -->
                <div style="margin-left: 10px">
                  <div class="system-date">
                    {{item.createTime}}
                  </div>
                  <div class="system-content">
                    {{item.content}}
                  </div>
                </div>
              </div>
            </template>
            <div v-else class="myCenter" style="height: 100%;color: var(--greyFont)">
              暂无系统消息
            </div>
          </div>
        </div>
        <!-- 聊天 -->
        <div class="body-right chat-container"
             v-if="subType === 2 && (!$common.isEmpty(currentChatFriendId) || !$common.isEmpty(currentChatGroupId))">
          <chat :currentChatFriendId="currentChatFriendId"
                :currentChatGroupId="currentChatGroupId"
                :friends="friends"
                :groups="groups"
                :imageList="imageList"
                :imMessages="imMessages"
                :groupMessages="groupMessages"
                :showMobileBackBtn="isMobile() && !showBodyLeft"
                @sendMsg="sendMsg"
                @openFriendCircle="openFriendCircle"
                @backToList="showChatList"></chat>
        </div>
        <div class="body-right" v-if="subType === 3">
          <div style="height: 60px;background-color: var(--maxWhite);display: flex;align-items: center;">
            <!-- 移动端返回按钮 -->
            <div v-if="isMobile() && !showBodyLeft" class="mobile-back-btn" @click="showChatList()">
              <el-icon><ArrowLeft /></el-icon>
            </div>
            <span style="line-height: 60px;margin-left: 20px;font-size: 18px">
              好友请求
            </span>
          </div>
          <div class="friend-request">
            <div style="margin: 20px;display: flex"
                 v-for="(item, index) in friendRequests"
                 :key="index">
              <!-- 图标 -->
              <div>
                <el-avatar fit="cover"
                          :size="50"
                          lazy
                          :src="$common.getAvatarUrl(item.avatar)"
                          :fallback-src="$common.getDefaultAvatar()"/>
              </div>
              <!-- 内容 -->
              <div style="display: flex;flex: 1">
                <div style="margin-left: 10px">
                  <div style="font-size: 16px;margin-bottom: 4px">
                    {{item.username}}
                  </div>
                  <div style="font-size: 12px;color: var(--greyFont)">
                    {{item.remark}}&nbsp;&nbsp;|&nbsp;&nbsp;{{item.createTime}}
                  </div>
                </div>
                <div class="request-status">
                  <el-button @click="changeFriendStatus(item.friendId, 1, index)" size="small" type="primary"
                            style="margin-right: 10px">
                    通过
                  </el-button>
                  <el-button @click="changeFriendStatus(item.friendId, -1, index)" size="small" type="error">
                    拒绝
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="body-right" v-if="subType === 4 && !$common.isEmpty(currentFriendId)">
          <div style="height: 60px;background-color: var(--maxWhite);display: flex;align-items: center;">
            <!-- 移动端返回按钮 -->
            <div v-if="isMobile() && !showBodyLeft" class="mobile-back-btn" @click="showChatList()">
              <el-icon><ArrowLeft /></el-icon>
            </div>
            <span style="line-height: 60px;margin-left: 20px;font-size: 18px">
              {{friends[currentFriendId].remark}}
            </span>
          </div>
          <div style="background: var(--midWhite);height: calc(100% - 60px)">
            <div class="myCenter">
              <div class="friend-info">
                <div style="display: flex;align-items: center">
                  <el-avatar fit="cover"
                            :size="60"
                            lazy
                            :src="$common.getAvatarUrl(friends[currentFriendId].avatar)"
                            :fallback-src="$common.getDefaultAvatar()"/>
                  <span style="margin: 0 5px 0 15px;font-size: 16px">{{friends[currentFriendId].remark}}</span>
                  <span style="cursor: pointer;margin-top: 6px" @click="changeDataType(1)">
                    <svg viewBox="0 0 1024 1024" width="20" height="20">
                      <path
                        d="M929.909189 827.019236H93.990821c-16.598379 0-29.997071 13.398692-29.99707 29.997071s13.398692 29.997071 29.99707 29.997071h835.918368c16.598379 0 29.997071-13.398692 29.99707-29.997071 0-16.498389-13.398692-29.997071-29.99707-29.997071z"
                        fill="#FF6600" opacity=".502"></path>
                      <path
                        d="M705.931061 198.080656c3.099697 0 8.999121 0.799922 14.098624 5.899424l28.297236 28.297237c5.099502 5.099502 5.899424 10.998926 5.899424 14.098623 0 3.099697-0.799922 8.999121-5.899424 14.098623L392.161703 616.739772l-86.991505 28.997168 27.597305-82.791915 358.964945-358.964945c5.099502-5.199492 11.098916-5.899424 14.198613-5.899424m0-59.994141c-20.497998 0-40.896006 7.799238-56.594473 23.397715L281.672493 529.148325l-0.699932-0.699931-70.693096 212.079289 212.079289-70.693097 0.699932 0.699932 367.664095-367.664095c31.196953-31.196953 31.196953-81.892003 0-113.088956l-28.297237-28.297237c-15.598477-15.598477-35.996485-23.397715-56.494483-23.397715z"
                        fill="#FF6600"></path>
                      <path d="M578.626494 230.803461L621.049351 188.381603l141.40619 141.406191-42.421857 42.421857z"
                            fill="#FF6600"></path>
                    </svg>
                  </span>
                </div>
                <div style="margin-top: 4px;cursor: pointer" @click="removeFriend(currentFriendId)">
                  <span style="vertical-align: -4px;margin-right: 2px">
                    <svg viewBox="0 0 1024 1024" width="20" height="20">
                      <path
                        d="M920 242.82H768v-75.45C768 128 736.66 96 698 96H326c-38.66 0-70 32-70 71.37v75.45H104c-22.09 0-40 18.26-40 40.79 0 22.52 17.91 40.78 40 40.78h88v532.24c0 39.42 31.34 71.37 70 71.37h500c38.66 0 70-32 70-71.37V324.39h88c22.09 0 40-18.26 40-40.78 0-22.53-17.91-40.79-40-40.79z m-584-24.47c0-22.52 17.91-40.78 40-40.78h272c22.09 0 40 18.26 40 40.78v24.47H336z m416 587.3c0 22.52-17.91 40.78-40 40.78H312c-22.09 0-40-18.26-40-40.78V324.39h480z"
                        fill="#999999"></path>
                      <path
                        d="M360 424a40 40 0 0 0-40 40v240a40 40 0 0 0 80 0V464a40 40 0 0 0-40-40zM512 424a40 40 0 0 0-40 40v240a40 40 0 0 0 80 0V464a40 40 0 0 0-40-40zM624 464v240a40 40 0 0 0 80 0V464a40 40 0 0 0-80 0z"
                        fill="#999999"></path>
                    </svg>
                  </span>
                  <span style="color: var(--greyFont)">删除好友</span>
                </div>
              </div>
            </div>
            <el-divider/>
            <div class="myCenter">
              <div style="width: 65%;font-size: 16px">
                <div style="margin-bottom: 10px">
                  <span class="friend-label">用户名</span>
                  <span>{{friends[currentFriendId].username}}</span>
                </div>
                <div style="margin-bottom: 10px">
                  <span class="friend-label">
                    性&nbsp;&nbsp;&nbsp;别
                  </span>
                  <span>
                    <template v-if="friends[currentFriendId].gender === 1">
                      男
                    </template>
                    <template v-else-if="friends[currentFriendId].gender === 2">
                      女
                    </template>
                    <template v-else>
                      薛定谔的猫
                    </template>
                  </span>
                </div>
                <div>
                  <span class="friend-label">简&nbsp;&nbsp;&nbsp;介</span>
                  <span>{{$common.isEmpty(friends[currentFriendId].introduction)?'暂无简介':friends[currentFriendId].introduction}}</span>
                </div>
              </div>
            </div>
            <el-divider/>
            <div class="myCenter sendMsg">
              <el-button @click="sendFriendMessage()" type="info">
                发消息
              </el-button>
            </div>
          </div>
        </div>
        <!-- 群 -->
        <groupInfo class="body-right"
                   v-if="subType === 5 && !$common.isEmpty(currentGroupId)"
                   :groups="groups"
                   :currentGroupId="currentGroupId"
                   :showMobileBackBtn="isMobile() && !showBodyLeft"
                   @exitGroup="exitGroup"
                   @dissolveGroup="dissolveGroup"
                   @changeDataType="changeDataType"
                   @sendGroupMessage="sendGroupMessage"
                   @changeAvatar="changeAvatar"
                   @backToList="showChatList"></groupInfo>
        <!-- 全屏对话框 - teleport到body确保样式正确 -->
        <teleport to="body">
          <!-- 头像修改弹出框 -->
          <el-dialog class="avatar-upload-dialog" v-model="showAvatarDialog" width="30%" :show-close="true" :append-to-body="true" :close-on-click-modal="true" align-center>
            <div style="padding: 40px;background: var(--background);border-radius: 5px;">
              <div style="margin: 0 0 25px;text-align: center;font-size: 18px;font-weight: bold;color: var(--fontColor)">上传图片</div>
              <uploadPicture :prefix="avatarPrefix" @addPicture="submitAvatar" :maxSize="3"
                             :maxNumber="1"></uploadPicture>
            </div>
          </el-dialog>
          <!-- 随笔对话框 -->
          <el-dialog class="weiyan-dialog" v-model="weiYanDialogVisible" width="auto" :close-on-click-modal="true" :show-close="true" :append-to-body="true" destroy-on-close align-center>
            <div class="weiyan-edit">
              <div class="myCenter" style="padding-bottom: 20px">
                <el-radio-group v-model="isPublic">
                  <el-radio-button :label="true">公开</el-radio-button>
                  <el-radio-button :label="false">私密</el-radio-button>
                </el-radio-group>
              </div>
              <commentBox @submitComment="submitWeiYan"></commentBox>
            </div>
          </el-dialog>
        </teleport>
        <!-- 其他弹窗teleport到friend-wrap确保遮罩正确覆盖 -->
        <!-- 使用:disabled延迟teleport直到组件挂载完成，避免目标元素不存在的时序问题 -->
        <teleport to=".friend-wrap" :disabled="!isMounted">
          <!-- 修改信息 -->
          <el-dialog class="friend-model" :auto-focus="false" v-model="changeModal" width="30%" :show-close="false" :append-to-body="false">
            <div style="background-color: var(--background);padding: 20px;border-radius: 5px">
              <div style="text-align: center;font-size: 20px;padding: 20px">
                修改信息
              </div>
              <div>
                <el-input v-model="changeData" maxlength="30" show-count clearable/>
              </div>
              <div class="myCenter" style="margin-top: 30px">
                <el-button @click="submitChange()" type="info">
                  提交
                </el-button>
              </div>
            </div>
          </el-dialog>
          <!-- 朋友圈 -->
          <el-dialog class="friend-model friend-circle-dialog" v-model="showFriendCircle" :close-on-click-modal="false" width="70%" top="5vh" :show-close="false" :append-to-body="true">
            <div style="background-color: var(--background);border-radius: 10px">
              <div class="treeHole-wrap">
                <template v-if="treeHoleList && treeHoleList.length > 0">
                  <treeHole :avatar="weiYanAvatar"
                            :treeHoleList="treeHoleList"
                            :viewUserId="pagination.userId"
                            :showLaunchButton="false"
                            @launch="launch"
                            @deleteTreeHole="deleteTreeHole"></treeHole>
                </template>
                <div v-else class="myCenter" style="height: 100%; color: var(--greyFont)">
                  暂无任何内容
                </div>
              </div>
              <div class="pagination-wrap" style="display: flex; justify-content: center; padding-bottom: 20px;">
                <proButton :info="'关闭'"
                           @click="cleanFriendCircle()"
                           before="black"
                           after="linear-gradient(45deg, #f43f3b, #ec008c)">
                </proButton>
                <proButton :info="'下一页'"
                           style="margin-left: 20px"
                           @click="pageWeiYan()"
                           v-if="pagination.total !== treeHoleList.length"
                           before="linear-gradient(45deg, #f43f3b, #ec008c)"
                           after="rgb(180, 203, 255)">
                </proButton>
                <proButton :info="'随笔'"
                           style="margin-left: 20px"
                           v-if="$store.state.currentUser.id === pagination.userId"
                           @click="launch()"
                           before="rgb(131, 123, 199)"
                           after="linear-gradient(45deg, #f43f3b, #ec008c)">
                </proButton>
                <proButton :info="'加好友'"
                           style="margin-left: 20px"
                           v-else-if="$common.isEmpty(friends[pagination.userId])"
                           @click="addFriend()"
                           before="rgb(131, 123, 199)"
                           after="linear-gradient(45deg, #f43f3b, #ec008c)">
                </proButton>
              </div>
            </div>
          </el-dialog>
          <!-- 绑定邮箱 -->
          <el-dialog class="friend-model" v-model="emailVisible" :show-close="false" :append-to-body="false">
            <div class="email-wrap">
              <div class="email-title">请绑定邮箱，接收留言通知</div>
              <div>
                <div style="margin-bottom: 5px">邮箱：</div>
                <el-input v-model="email"></el-input>
                <div style="margin-top: 10px;margin-bottom: 5px">验证码：</div>
                <el-input v-model="code"></el-input>
                <div style="margin-top: 10px;margin-bottom: 5px">密码：</div>
                <el-input v-model="password" type="password" show-password></el-input>
              </div>
              <div class="myCenter" style="margin-top: 30px">
                <proButton :info="codeString"
                           @click="getCode()"
                           :before="$constant.before_color_1"
                           :after="$constant.after_color_1"
                           style="margin-right: 20px">
                </proButton>
                <proButton :info="'提交'"
                           @click="submitDialog()"
                           :before="$constant.before_color_2"
                           :after="$constant.after_color_2">
                </proButton>
              </div>
            </div>
          </el-dialog>
        </teleport>
      </div>
    </div>
    <div id="outerImg">
      <div id="innerImg" style="position:absolute">
        <img id="bigImg" src=""/>
      </div>
    </div>
  </div>
</template>
<script>
  import Im from "../../utils/imUtils/im";
  import { useMainStore } from '../../stores/main';
  import { useImStore } from '../../stores/im';
  import {nextTick} from 'vue';
  import { message as ElMessage, confirm as ElMessageBoxConfirm, prompt as ElMessageBoxPrompt, notification as ElNotification } from '@/utils/element-command-services';
  const ElMessageBox = { confirm: ElMessageBoxConfirm, prompt: ElMessageBoxPrompt };
  import {ArrowLeft} from "@element-plus/icons-vue";
  import bindEmail from "./hooks/bindEmail";
  import friendCircle from "./hooks/friendCircle";
  import friend from "./hooks/friend";
  import group from "./hooks/group";
  import imUtil from "./hooks/imUtil";
  import changeData from "./hooks/changeData";
  import { getMessagePreview } from "../../utils/imUtils/messagePreview";
  import { loadFonts } from "../../utils/font-loader";
  import { formatChatTime } from "../../utils/chat-time-utils";
  import proButton from "./common/proButton";
  import treeHole from "./common/treeHole";
  import commentBox from "./common/commentBox";
  import uploadPicture from "./common/uploadPicture";
  import chat from "./common/chat";
  import groupInfo from "./common/groupInfo";
  import {reactive, computed, getCurrentInstance, onMounted, onBeforeUnmount, watchEffect, toRefs} from 'vue';
  export default {
    components: {
      proButton,
      treeHole,
      commentBox,
      uploadPicture,
      chat,
      groupInfo,
      ArrowLeft
    },
    setup(props, context) {
      const globalProperties = getCurrentInstance().appContext.config.globalProperties;
      const $common = globalProperties.$common;
      const $http = globalProperties.$http;
      const $constant = globalProperties.$constant;
      const mainStore = useMainStore();
      const imStore = useImStore();
      const router = useRouter();

      // 检查聊天室功能是否开启
      watchEffect(() => {
        if (mainStore.sysConfig && mainStore.sysConfig['im.enable'] === 'false') {
          ElMessage({
            message: '聊天室功能已关闭',
            type: 'warning'
          });
          router.push('/');
        }
      });

      // 创建兼容层，让模板中的 $store.state 可以继续工作
      const store = {
        state: {
          get currentUser() { return mainStore.currentUser; },
          get sysConfig() { return mainStore.sysConfig; },
          get imMessages() { return imStore.imMessages; },
          get groupMessages() { return imStore.groupMessages; },
          get imChats() { return imStore.imChats; },
          get groupChats() { return imStore.groupChats; },
          get imMessageBadge() { return imStore.imMessageBadge; },
          get groupMessageBadge() { return imStore.groupMessageBadge; },
          get lastMessagePreviews() { return imStore.lastMessagePreviews; },
          get onlineUserCount() { return imStore.onlineUserCount; },
          get systemMessageBadge() { return imStore.systemMessageBadge; },
        },
        commit(mutation, payload) {
          // 将 Vuex commit 映射到 Pinia action
          switch(mutation) {
            case 'loadCurrentUser': mainStore.loadCurrentUser(payload); break;
            case 'loadSysConfig': mainStore.loadSysConfig(payload); break;
            case 'updateImChats': imStore.updateImChats(payload); break;
            case 'updateGroupChats': imStore.updateGroupChats(payload); break;
            case 'updateImMessages': imStore.updateImMessages(payload.friendId, payload.messages); break;
            case 'updateGroupMessages': imStore.updateGroupMessages(payload.groupId, payload.messages); break;
            case 'addImMessage': imStore.addImMessage(payload.friendId, payload.message); break;
            case 'addGroupMessage': imStore.addGroupMessage(payload.groupId, payload.message); break;
            case 'updateImMessageBadge': imStore.updateImMessageBadge(payload.friendId, payload.count); break;
            case 'updateGroupMessageBadge': imStore.updateGroupMessageBadge(payload.groupId, payload.count); break;
            case 'updateSystemMessageBadge': imStore.updateSystemMessageBadge(payload); break;
            case 'updateLastMessagePreview': imStore.updateLastMessagePreview(payload.chatId, payload.preview); break;
            case 'updateOnlineUserCount': imStore.updateOnlineUserCount(payload.groupId, payload.count); break;
            case 'resetSystemMessageBadge': imStore.resetSystemMessageBadge(); break;
            default: console.warn('Unknown mutation:', mutation);
          }
        }
      };
      const {bindEmailData, getCode, submitDialog} = bindEmail();
      const {friendCircleData, launch, openFriendCircle, deleteTreeHole, submitWeiYan, pageWeiYan, cleanFriendCircle, addFriend} = friendCircle();
      const {friendData, getImFriend, removeFriend, getFriendRequests, changeFriendStatus} = friend();
      const {groupData, getImGroup, addGroupTopic, exitGroup, dissolveGroup, removeGroupFromList} = group();
      const {imUtilData, changeAside, mobileRight, getSystemMessages, markSystemMessagesAsRead, hiddenBodyLeft, imgShow, getImageList, parseMessage, updateAsideActiveState, saveShowBodyLeftState, isMobile} = imUtil();
      const {changeDataData, changeAvatar, changeDataType, submitAvatar, submitChange} = changeData(friendData, groupData);
      
      let data = reactive({
        //当前聊天信息
        currentChatFriendId: null,
        //当前群聊天信息
        currentChatGroupId: null,
        //1:消息 2:好友 3:群组 4:系统
        type: 1,
        subType: 1,
        showFriendValue: '',
        // 防止重复调用在线人数接口
        lastOnlineCountGroupId: null,
        onlineCountTimer: null,
        // 暗色模式状态（与博客前端保持一致）
        isDarkMode: false,
        // 延迟teleport激活直到挂载完成
        isMounted: false,
        // 每分钟更新一次，用于触发时间显示刷新
        minuteTick: 0
      })
      // 使用computed从store获取聊天数据，实现响应式
      const imMessages = computed(() => store.state.imMessages);
      const imMessageBadge = computed(() => store.state.imMessageBadge);
      const imChats = computed(() => store.state.imChats);
      const groupMessages = computed(() => store.state.groupMessages);
      const groupMessageBadge = computed(() => store.state.groupMessageBadge);
      const groupChats = computed(() => store.state.groupChats);
      const lastMessagePreviews = computed(() => store.state.lastMessagePreviews);
      let im;
      if (!$common.isEmpty(store.state.currentUser)) {
        // 并行执行非关键初始化操作，提升页面加载速度
        Promise.all([
          getImageList(),
          getSystemMessages(), 
          getFriendRequests(),
          getSysConfig()
        ]).catch(error => {
          console.warn('部分初始化操作失败，但不影响核心功能:', error);
        });
        // 关键的好友和群组数据加载
        getFriendAndGroup();
      }
      async function getFriendAndGroup() {
        // 并行加载好友和群组数据，提升加载速度
        const [friendResult, groupResult] = await Promise.all([
          getImFriend(),
          getImGroup()
        ]);
        await nextTick();
        await getIm();
      }
      function getSysConfig() {
        $http.get($constant.baseURL + "/sysConfig/listSysConfig")
          .then((res) => {
            if (!$common.isEmpty(res.data)) {
              store.commit("loadSysConfig", res.data);
              buildCssPicture();
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      }
      async function buildCssPicture() {
        let root = document.querySelector(":root");
        let webStaticResourcePrefix = store.state.sysConfig['webStaticResourcePrefix'];
        root.style.setProperty("--commentURL", "url(" + webStaticResourcePrefix + "assets/commentURL.jpg)");
        root.style.setProperty("--imBackground", "url(" + webStaticResourcePrefix + "assets/backgroundPicture.jpg)");
        // 使用新的字体加载器
        try {
          await loadFonts(store.state.sysConfig);
          console.log('字体加载完成');
        } catch (error) {
          console.warn('字体加载失败，使用系统默认字体:', error);
        }
      }
      async function getIm() {
        im = new Im();
        await im.initWs();
        
        // initWs 是异步的，完成后 im.tio 才会被初始化
        if (!im.tio || !im.tio.ws) {
          console.error('[WebSocket] 初始化失败，im.tio 或 im.tio.ws 为空');
          return;
        }
        
        im.tio.ws.onmessage = function (event) {
          let message;
          try {
            message = JSON.parse(event.data);
          } catch (error) {
            console.error('JSON解析失败:', error);
            console.error('原始消息内容:', event.data);
            // 尝试清理并重新解析
            try {
              const cleanedData = event.data.trim();
              message = JSON.parse(cleanedData);
            } catch (e) {
              console.error('清理后仍然无法解析，忽略此消息');
              return;
            }
          }
          // 更新心跳响应时间（重要：每次收到消息都要调用）
          if (im.onMessageReceived) {
            im.onMessageReceived();
          }
          // 处理被踢出消息（优先级最高）
          if (message.messageType === 999) {
            console.warn('[WebSocket] 收到被踢出消息:', message.content);
            im.isKickedByDuplicate = true; // 设置被踢出标志
            ElMessage({
              message: message.content || "您的账号在其他地方登录，当前连接已断开",
              type: 'warning',
              duration: 0, // 不自动关闭
              showClose: true,
              customClass: 'duplicate-connection-warning'
            });
            return;
          }
          // 处理系统消息相关逻辑 (fromId 为 -1)
          if (message.fromId === -1) {
            console.log('[WebSocket] 收到系统级别消息:', message);
            // 1. 如果 toId 也是 -1，代表是发给所有人的全局“系统消息”（主界面的系统消息选项卡）
            if (message.toId === -1) {
              const currentBadge = store.state.systemMessageBadge || 0;
              store.commit('updateSystemMessageBadge', currentBadge + 1);
              getSystemMessages(); // 刷新列表
              ElNotification({
                title: '系统通知',
                message: message.content,
                type: 'info',
                offset: 60
              });
              return;
            }
            // 2. 如果 toId 不是 -1，或者是群聊消息中的系统内容，将在下方常规流程中处理渲染
          }
          message.content = parseMessage(message.content);
          // 处理UI状态同步消息
          if (message.messageType === 4) {
            console.log('[WebSocket] 收到UI状态同步消息:', message);
            if (message.showBodyLeft !== undefined) {
              imUtilData.showBodyLeft = message.showBodyLeft;
              mobileRight(true); // 跳过动画，避免闪烁
            }
            return;
          }
          // 处理同步消息（聊天列表+未读数+最后一条消息）
          if (message.messageType === 5) {
            console.log('[WebSocket] 收到同步数据:', message);
            // 更新私聊列表
            if (message.friendChatList && Array.isArray(message.friendChatList)) {
              store.commit('updateImChats', message.friendChatList);
            }
            // 更新群聊列表
            if (message.groupChatList && Array.isArray(message.groupChatList)) {
              store.commit('updateGroupChats', message.groupChatList);
            }
            // 批量更新好友未读数
            if (message.friendUnreadCounts) {
              Object.keys(message.friendUnreadCounts).forEach(friendId => {
                const count = message.friendUnreadCounts[friendId];
                store.commit('updateImMessageBadge', {friendId: parseInt(friendId), count});
              });
            }
            // 批量更新群聊未读数
            if (message.groupUnreadCounts) {
              Object.keys(message.groupUnreadCounts).forEach(groupId => {
                const count = message.groupUnreadCounts[groupId];
                store.commit('updateGroupMessageBadge', {groupId: parseInt(groupId), count});
              });
            }
            // 处理好友聊天最后一条消息预览
            if (message.friendLastMessages) {
              Object.keys(message.friendLastMessages).forEach(friendId => {
                const preview = message.friendLastMessages[friendId];
                if (preview && preview.content) {
                  // 解析消息内容（表情、图片等）
                  const parsedPreview = {
                    content: parseMessage(preview.content),
                    createTime: preview.createTime
                  };
                  store.commit('updateLastMessagePreview', {chatId: `friend_${friendId}`, preview: parsedPreview});
                }
              });
            }
            // 处理群聊最后一条消息预览
            if (message.groupLastMessages) {
              Object.keys(message.groupLastMessages).forEach(groupId => {
                const preview = message.groupLastMessages[groupId];
                if (preview && preview.content) {
                  // 解析消息内容（表情、图片等）
                  const parsedPreview = {
                    content: parseMessage(preview.content),
                    createTime: preview.createTime
                  };
                  store.commit('updateLastMessagePreview', {chatId: `group_${groupId}`, preview: parsedPreview});
                }
              });
            }
            return;
          }
          if (message.messageType === 1) {
            if (message.fromId === store.state.currentUser.id && (friendData.friends[message.toId] !== null && friendData.friends[message.toId] !== undefined)) {
              // 添加消息到store
              store.commit('addImMessage', {friendId: message.toId, message});
              // 更新最后消息预览
              store.commit('updateLastMessagePreview', {
                chatId: `friend_${message.toId}`, 
                preview: {content: message.content, createTime: message.createTime}
              });
              // 更新聊天列表顺序
              const currentChats = [...imChats.value];
              const existingIndex = currentChats.indexOf(message.toId);
              if (existingIndex > -1) {
                currentChats.splice(existingIndex, 1);
              }
              currentChats.unshift(message.toId);
              store.commit('updateImChats', currentChats);
              // 检查元素是否存在再调用 isActive
              const userElement = document.getElementsByClassName('im-user-current')[0];
              if (userElement) {
                isActive(userElement, 'im-active', null, 2, message.toId, 2);
              }
            } else if (message.fromId !== store.state.currentUser.id && (friendData.friends[message.fromId] !== null && friendData.friends[message.fromId] !== undefined)) {
              // 添加消息到store
              store.commit('addImMessage', {friendId: message.fromId, message});
              // 更新最后消息预览
              store.commit('updateLastMessagePreview', {
                chatId: `friend_${message.fromId}`, 
                preview: {content: message.content, createTime: message.createTime}
              });
              // 更新聊天列表顺序
              const currentChats = [...imChats.value];
              const existingIndex = currentChats.indexOf(message.fromId);
              if (existingIndex > -1) {
                currentChats.splice(existingIndex, 1);
              }
              currentChats.unshift(message.fromId);
              store.commit('updateImChats', currentChats);
              if (data.subType !== 2 || data.currentChatFriendId !== message.fromId) {
                // 不是当前聊天对象，更新未读消息数
                const currentBadge = imMessageBadge.value[message.fromId] || 0;
                store.commit('updateImMessageBadge', {friendId: message.fromId, count: currentBadge + 1});
                // 移动端收到新消息时，如果当前不在聊天界面，显示左侧面板
                if (isMobile() && data.subType !== 2) {
                  imUtilData.showBodyLeft = true;
                  mobileRight(true); // 跳过动画
                  saveShowBodyLeftState();
                }
              } else {
                // 是当前聊天对象，自动标记为已读
                $http.post($constant.baseURL + "/imChatGroup/markFriendAsRead?friendId=" + message.fromId, null, false)
                  .catch(error => {
                    console.warn('收到消息后自动标记好友消息已读失败:', error);
                  });
              }
            }
            nextTick(() => {
              let msgContainer = document.getElementsByClassName('msg-container');
              if (msgContainer && msgContainer.length > 0) {
                msgContainer[0].scrollTop = msgContainer[0].scrollHeight;
              }
              imgShow();
            });
          } else if (message.messageType === 2 && (groupData.groups[message.groupId] !== null && groupData.groups[message.groupId] !== undefined)) {
            // 添加群消息到store
            store.commit('addGroupMessage', {groupId: message.groupId, message});
            // 更新最后消息预览
            store.commit('updateLastMessagePreview', {
              chatId: `group_${message.groupId}`, 
              preview: {content: message.content, createTime: message.createTime}
            });
            // 检查该群是否在聊天列表中（可能被隐藏了）
            const currentChats = [...groupChats.value];
            const isInList = currentChats.includes(message.groupId);
            if(message.fromId === store.state.currentUser.id || !groupMessages.value[message.groupId]) {
              // 更新群聊列表顺序
              const existingIndex = currentChats.indexOf(message.groupId);
              if (existingIndex > -1) {
                currentChats.splice(existingIndex, 1);
              }
              currentChats.unshift(message.groupId);
              store.commit('updateGroupChats', currentChats);
              // 检查元素是否存在再调用 isActive
              const groupElement = document.getElementsByClassName('im-group-current')[0];
              if (groupElement) {
                isActive(groupElement, 'im-active', null, 2, message.groupId, 1);
              }
            } else if (!isInList) {
              // 群不在列表中（被隐藏），自动取消隐藏并添加到列表顶部
              currentChats.unshift(message.groupId);
              store.commit('updateGroupChats', currentChats);
            }
            if (message.fromId !== store.state.currentUser.id) {
              if (data.subType !== 2 || data.currentChatGroupId !== message.groupId) {
                // 不是当前聊天对象，更新未读消息数
                const currentBadge = groupMessageBadge.value[message.groupId] || 0;
                store.commit('updateGroupMessageBadge', {groupId: message.groupId, count: currentBadge + 1});
                // 移动端收到新群消息时，如果当前不在聊天界面，显示左侧面板
                if (isMobile() && data.subType !== 2) {
                  imUtilData.showBodyLeft = true;
                  mobileRight(true); // 跳过动画
                  saveShowBodyLeftState();
                }
              } else {
                // 是当前聊天群组，自动标记为已读
                $http.post($constant.baseURL + "/imChatGroup/markGroupAsRead?groupId=" + message.groupId, null, false)
                  .catch(error => {
                    console.warn('收到消息后自动标记群消息已读失败:', error);
                  });
              }
            }
            nextTick(() => {
              let msgContainer = document.getElementsByClassName('msg-container');
              if (msgContainer && msgContainer.length > 0) {
                msgContainer[0].scrollTop = msgContainer[0].scrollHeight;
              }
              imgShow();
            });
          } else if (message.messageType === 3) {
            // 处理在线用户数更新消息
            if (message.groupId && message.onlineCount !== undefined) {
              store.commit('updateOnlineUserCount', {
                groupId: message.groupId,
                count: message.onlineCount
              });
            }
          }
        }
      }
      function sendMsg(msg, callback) {
        // 检查WebSocket连接状态
        if (!im.tio || !im.tio.isReady()) {
          ElMessage({
            message: "连接已断开，消息发送失败！请检查网络连接。",
            type: 'error',
            duration: 4000
          });
          callback(false);
          return;
        }
        let success = im.sendMsg(msg);
        // 如果发送失败，给用户明确提示
        if (!success) {
          ElMessage({
            message: "消息发送失败！请检查网络连接或稍后重试。",
            type: 'error',
            duration: 3000
          });
        }
        // 发送成功后，如果是群聊消息，自动标记该群为已读（更新last_read_time）
        if (success) {
          try {
            const parsedMsg = JSON.parse(msg);
            // messageType: 2 = 群聊消息
            if (parsedMsg.messageType === 2 && parsedMsg.groupId) {
              $http.post($constant.baseURL + "/imChatGroup/markGroupAsRead?groupId=" + parsedMsg.groupId, null, false)
                .catch(error => {
                  console.warn('发送消息后自动标记群消息已读失败:', error);
                });
            }
            // messageType: 1 = 私聊消息
            else if (parsedMsg.messageType === 1 && parsedMsg.toId) {
              $http.post($constant.baseURL + "/imChatGroup/markFriendAsRead?friendId=" + parsedMsg.toId, null, false)
                .catch(error => {
                  console.warn('发送消息后自动标记好友消息已读失败:', error);
                });
            }
          } catch (e) {
            console.warn('解析消息类型失败，跳过自动标记已读:', e);
          }
        }
        callback(success);
      }
      function isActive(e, className, type, subType, current, imType) {
        // 空值检查：如果元素不存在，直接返回
        if (!e) {
          console.warn('[聊天室] isActive 调用时元素不存在，跳过处理');
          return;
        }
        // 检查是否是侧边栏按钮的重复点击
        let isRepeatClick = false;
        if (!$common.isEmpty(type)) {
          // 检查当前点击的按钮是否已经是激活状态
          let currentElement = e instanceof HTMLElement ? e : e.currentTarget;
          if (currentElement && currentElement.classList && currentElement.classList.contains('aside-active') && data.type === type) {
            isRepeatClick = true;
          }
          data.type = type;
          // 先清除所有内容区域的激活状态
          let actives = ["im-active", "friend-active", "im-group"];
          for (let activeClass of actives) {
            for (let tab of document.getElementsByClassName(activeClass)) {
              tab.classList.remove(activeClass);
            }
          }
        }
        if (!$common.isEmpty(subType)) {
          data.subType = subType;
          if (subType === 1) {
            markSystemMessagesAsRead();
          }
          if (subType === 4 && !$common.isEmpty(current)) {
            friendData.currentFriendId = current.friendId;
          }
          if (subType === 5 && !$common.isEmpty(current)) {
            groupData.currentGroupId = current.id;
          }
          if (subType === 2 && !$common.isEmpty(current) && !$common.isEmpty(imType)) {
            console.log(`[${isMobile() ? '移动端' : 'PC端'}] 进入聊天界面 - imType: ${imType}, current: ${current}`);
            if (imType === 1) {
              data.currentChatFriendId = null;
              data.currentChatGroupId = current;
              // 清零群聊未读消息数
              store.commit('updateGroupMessageBadge', {groupId: current, count: 0});
              // 调用后端接口标记群消息为已读
              $http.post($constant.baseURL + "/imChatGroup/markGroupAsRead?groupId=" + current, null, false)
                .catch(error => {
                  console.warn('标记群消息已读失败:', error);
                });
              console.log(`[${isMobile() ? '移动端' : 'PC端'}] 设置当前群聊ID: ${current}`);
              // 获取群组消息
              getGroupMessages(current);
              // 延迟获取在线人数，避免时序问题（等待旧连接完全关闭）
              setTimeout(() => {
                getGroupOnlineCountWithDebounce(current);
              }, 500);
              
              if (groupData.groups[current] && groupData.groups[current].groupType === 2) {
                addGroupTopic();
              }
            } else if (imType === 2) {
              data.currentChatGroupId = null;
              data.currentChatFriendId = current;
              // 清零私聊未读消息数
              store.commit('updateImMessageBadge', {friendId: current, count: 0});
              // 调用后端接口标记好友消息为已读
              $http.post($constant.baseURL + "/imChatGroup/markFriendAsRead?friendId=" + current, null, false)
                .catch(error => {
                  console.warn('标记好友消息已读失败:', error);
                });
              console.log(`[${isMobile() ? '移动端' : 'PC端'}] 设置当前好友ID: ${current}`);
              // 获取好友消息
              getMessages(current);
            }
          }
          // 移动端界面切换逻辑优化
          if (isMobile()) {
            if (subType === 1 || subType === 2 || subType === 3 || subType === 4 || subType === 5) {
              // 进入任何右侧详情/聊天/消息页面时隐藏左侧面板
              imUtilData.showBodyLeft = false;
              saveShowBodyLeftState();
            }
          }
          nextTick(() => {
            let msgContainer = document.getElementsByClassName('msg-container');
            if (msgContainer && msgContainer.length > 0) {
              msgContainer[0].scrollTop = msgContainer[0].scrollHeight;
            }
            imgShow();
            // 进入聊天界面时跳过动画，避免消息发送后的页面进入动画
            if (subType === 2 && isMobile()) {
              mobileRight(true);
            } else {
              mobileRight();
            }
            hiddenBodyLeft();
          });
        }
        // 处理侧边栏按钮的切换逻辑
        if (!$common.isEmpty(type) && className === 'aside-active') {
          // 使用专门的函数更新侧边栏按钮状态
          updateAsideActiveState(type);
          if (isRepeatClick && isMobile()) {
            // 移动端重复点击同一个按钮，切换 body-left 显示状态
            imUtilData.showBodyLeft = !imUtilData.showBodyLeft;
          } else {
            // 点击不同按钮或桌面端
            if (isMobile()) {
              // 移动端：点击侧边栏按钮时显示左侧面板
              imUtilData.showBodyLeft = true;
            } else {
              // 桌面端：确保 body-left 显示
              imUtilData.showBodyLeft = true;
            }
          }
          mobileRight();
        } else {
          // 为内容区域项目添加激活状态
          for (const tab of document.getElementsByClassName(className)) {
            tab.classList.remove(className);
          }
          // 添加激活状态到当前元素
          if (e instanceof HTMLElement) {
            e.classList.add(className);
          } else if (e && e.currentTarget) {
            let node = e.currentTarget;
            node.classList.add(className);
          } else {
            console.warn('[聊天室] isActive: 无法获取currentTarget，跳过激活状态设置');
          }
        }
      }
      async function sendFriendMessage() {
        // 更新聊天列表顺序
        const currentChats = [...imChats.value];
        const existingIndex = currentChats.indexOf(friendData.currentFriendId);
        if (existingIndex > -1) {
          currentChats.splice(existingIndex, 1);
        }
        currentChats.unshift(friendData.currentFriendId);
        store.commit('updateImChats', currentChats);
        await nextTick();
        // 切换到聊天标签页
        isActive(document.getElementById('chat'), 'aside-active', 1);
        // 等待DOM更新完成，确保v-show生效
        await nextTick();
        // 使用setTimeout确保DOM完全渲染
        setTimeout(() => {
          // 查找对应的聊天项目元素（第一个元素，因为我们刚刚把它移到了数组开头）
          const userElements = document.getElementsByClassName('im-user-current');
          if (userElements && userElements.length > 0) {
            // 进入聊天界面
            isActive(userElements[0], 'im-active', null, 2, friendData.currentFriendId, 2);
          } else {
            console.warn('未找到聊天用户元素，当前type:', imUtilData.type);
          }
          getMessages(friendData.currentFriendId);
        }, 50);
      }
      async function sendGroupMessage() {
        // 更新群聊列表顺序
        const currentChats = [...groupChats.value];
        const existingIndex = currentChats.indexOf(groupData.currentGroupId);
        if (existingIndex > -1) {
          currentChats.splice(existingIndex, 1);
        }
        currentChats.unshift(groupData.currentGroupId);
        store.commit('updateGroupChats', currentChats);
        await nextTick();
        // 切换到聊天标签页
        isActive(document.getElementById('chat'), 'aside-active', 1);
        // 等待DOM更新完成，确保v-show生效
        await nextTick();
        // 使用setTimeout确保DOM完全渲染
        setTimeout(() => {
          // 查找对应的群聊项目元素（第一个元素，因为我们刚刚把它移到了数组开头）
          const groupElements = document.getElementsByClassName('im-group-current');
          if (groupElements && groupElements.length > 0) {
            // 进入群聊界面
            isActive(groupElements[0], 'im-active', null, 2, groupData.currentGroupId, 1);
          } else {
            console.warn('未找到群聊元素，当前type:', imUtilData.type);
          }
          console.log(`[${isMobile() ? '移动端' : 'PC端'}] 准备获取群组消息，群组ID:`, groupData.currentGroupId);
          getGroupMessages(groupData.currentGroupId);
          getGroupOnlineCountWithDebounce(groupData.currentGroupId);
          if (groupData.groups[groupData.currentGroupId] && groupData.groups[groupData.currentGroupId].groupType === 2) {
            addGroupTopic();
          }
        }, 50);
      }
      function getMessages(friendId, current = 1, size = 100) {
        if (!imMessages.value.hasOwnProperty(friendId)) {
          $http.get($constant.baseURL + "/imChatUserMessage/listFriendMessage", {
            friendId: friendId,
            current: current,
            size: size
          })
            .then((res) => {
              if (!$common.isEmpty(res.data) && !$common.isEmpty(res.data.records)) {
                res.data.records.forEach(message => {
                  message.content = parseMessage(message.content);
                });
                store.commit('updateImMessages', {friendId, messages: res.data.records});
              } else {
                store.commit('updateImMessages', {friendId, messages: []});
              }
              nextTick(() => {
                let msgContainer = document.getElementsByClassName('msg-container');
                if (msgContainer && msgContainer.length > 0) {
                  msgContainer[0].scrollTop = msgContainer[0].scrollHeight;
                }
                imgShow();
              });
            })
            .catch((error) => {
              ElMessage({
                message: error.message,
                type: 'error'
              });
            });
        }
      }
      function getGroupMessages(groupId, current = 1, size = 100) {
        if (!groupMessages.value.hasOwnProperty(groupId)) {
          $http.get($constant.baseURL + "/imChatUserGroupMessage/listGroupMessage", {
            groupId: groupId,
            current: current,
            size: size
          })
            .then((res) => {
              if (!$common.isEmpty(res.data) && !$common.isEmpty(res.data.records)) {
                res.data.records.forEach(message => {
                  message.content = parseMessage(message.content);
                });
                store.commit('updateGroupMessages', {groupId, messages: res.data.records});
              } else {
                store.commit('updateGroupMessages', {groupId, messages: []});
              }
              nextTick(() => {
                let msgContainer = document.getElementsByClassName('msg-container');
                if (msgContainer && msgContainer.length > 0) {
                  msgContainer[0].scrollTop = msgContainer[0].scrollHeight;
                }
                imgShow();
              });
            })
            .catch((error) => {
              ElMessage({
                message: error.message,
                type: 'error'
              });
            });
        }
        // 获取群组在线用户数 - 只有当前群组ID与要获取的群组ID一致时才调用
        if (data.currentChatGroupId === groupId) {
          getGroupOnlineCountWithDebounce(groupId);
        }
      }
      function getGroupOnlineCountWithDebounce(groupId) {
        if (groupId === null || groupId === undefined || groupId === '') {
          return;
        }
        // 如果是同一个群组且在短时间内重复调用，则跳过
        if (data.lastOnlineCountGroupId === groupId && data.onlineCountTimer) {
          console.log(`[${isMobile() ? '移动端' : 'PC端'}] 跳过重复的在线人数请求，群组ID: ${groupId}`);
          return;
        }
        // 清除之前的定时器
        if (data.onlineCountTimer) {
          clearTimeout(data.onlineCountTimer);
        }
        // 记录当前群组ID
        data.lastOnlineCountGroupId = groupId;
        // 设置防抖定时器
        data.onlineCountTimer = setTimeout(() => {
          getGroupOnlineCount(groupId);
          data.onlineCountTimer = null;
        }, 300); // 300ms 防抖
      }
      function getGroupOnlineCount(groupId) {
        if (groupId === null || groupId === undefined || groupId === '') {
          return;
        }
        // 注意：主动请求在线人数可能存在时序问题（重连时可能统计到多个连接）
        // 建议优先使用后端推送的在线人数更新（messageType=3）
        console.log(`[${isMobile() ? '移动端' : 'PC端'}] 开始获取群组 ${groupId} 的在线人数`);
        $http.get($constant.baseURL + "/imChatGroup/getOnlineCount", {
          groupId: groupId
        })
          .then((res) => {
            console.log(`[${isMobile() ? '移动端' : 'PC端'}] 获取在线人数响应:`, res);
            // 验证响应结构: {code: 200, data: number|string, ...}
            if (res && res.code === 200 && res.hasOwnProperty('data')) {
              const data = res.data;
              let onlineCount = 0;
              // 处理数据为数字的情况
              if (typeof data === 'number' && !isNaN(data)) {
                onlineCount = Math.max(0, data);
              }
              // 处理数据为字符串数字的情况
              else if (typeof data === 'string' && !isNaN(Number(data))) {
                onlineCount = Math.max(0, Number(data));
              }
              // 处理数据为null或undefined的情况
              else if (data === null || data === undefined) {
                onlineCount = 0;
              }
              // 处理其他情况，记录警告但不抛出错误
              else {
                console.warn('在线人数数据类型异常:', typeof data, data);
                onlineCount = 0;
              }
              store.commit('updateOnlineUserCount', {
                groupId: groupId,
                count: onlineCount
              });
              console.log(`[${isMobile() ? '移动端' : 'PC端'}] 群组 ${groupId} 在线人数更新为: ${onlineCount}`);
            } else {
              console.warn(`[${isMobile() ? '移动端' : 'PC端'}] 响应格式异常:`, res);
              // 设置默认值而不抛出错误
              store.commit('updateOnlineUserCount', {
                groupId: groupId,
                count: 0
              });
            }
          })
          .catch((error) => {
            console.error(`[${isMobile() ? '移动端' : 'PC端'}] 获取在线用户数失败:`, error);
            // 设置默认值
            store.commit('updateOnlineUserCount', {
              groupId: groupId,
              count: 0
            });
          });
      }
      // 显示聊天列表（移动端返回功能）
      function showChatList() {
        if (isMobile()) {
          console.log('[移动端] showChatList 调用 - 当前状态:', {
            showBodyLeft: imUtilData.showBodyLeft,
            subType: data.subType
          });
          // 防止事件冒泡导致的重复调用
          event?.stopPropagation();
          // 清除当前聊天状态
          data.currentChatFriendId = null;
          data.currentChatGroupId = null;
          // 重置到聊天列表状态
          data.subType = 1;
          // 显示左侧面板（聊天列表）
          imUtilData.showBodyLeft = true;
          // 更新移动端布局，使用动画
          mobileRight(false);
          // 确保侧边栏聊天按钮处于激活状态
          updateAsideActiveState(1);
          console.log('[移动端] showChatList 完成 - 新状态:', {
            showBodyLeft: imUtilData.showBodyLeft,
            subType: data.subType
          });
        }
      }
      // 移动端长按和PC端右键菜单相关变量
      let touchTimer = null;
      let touchStartTime = 0;
      const LONG_PRESS_DURATION = 800; // 长按时间阈值（毫秒）
      // 处理群聊项点击（区分普通点击和长按）
      function handleGroupChatClick(event, groupId) {
        // 如果是移动端且刚刚完成长按，则不执行点击
        if (isMobile() && touchTimer === 'completed') {
          touchTimer = null;
          return;
        }
        // 正常的群聊点击逻辑
        isActive(event, 'im-active', null, 2, groupId, 1);
      }
      // 处理触摸开始（移动端长按检测）
      function handleTouchStart(event, groupId) {
        if (!isMobile()) return;
        // 检查是否为系统消息，系统消息不支持删除
        if (event && event.currentTarget && event.currentTarget.classList && event.currentTarget.classList.contains('system-message')) {
          return;
        }
        // 阻止移动端的右键菜单和文本选择
        if (event && event.preventDefault) {
          event.preventDefault();
        }
        touchStartTime = Date.now();
        touchTimer = setTimeout(() => {
          // 长按触发删除菜单
          showGroupDeleteMenu(event, groupId);
          touchTimer = 'completed';
          // 添加触觉反馈（如果支持）
          if (navigator.vibrate) {
            navigator.vibrate(50);
          }
        }, LONG_PRESS_DURATION);
      }
      // 处理触摸结束
      function handleTouchEnd(event, groupId) {
        if (!isMobile()) return;
        const touchDuration = Date.now() - touchStartTime;
        if (touchTimer && touchTimer !== 'completed') {
          clearTimeout(touchTimer);
          touchTimer = null;
          // 如果触摸时间很短，认为是正常点击
          if (touchDuration < LONG_PRESS_DURATION) {
            // 延迟一点执行，确保长按逻辑不会干扰
            setTimeout(() => {
              handleGroupChatClick(event, groupId);
            }, 50);
          }
        }
      }
      // 处理右键菜单（PC端）
      function handleContextMenu(event, groupId) {
        // 移动端阻止右键菜单，避免与长按冲突
        if (isMobile()) {
          if (event && event.preventDefault) {
            event.preventDefault();
          }
          return;
        }
        // 检查是否为系统消息，系统消息不支持删除
        if (event && event.currentTarget && event.currentTarget.classList && event.currentTarget.classList.contains('system-message')) {
          if (event.preventDefault) {
            event.preventDefault();
          }
          return;
        }
        if (event && event.preventDefault) {
          event.preventDefault(); // 阻止默认右键菜单
        }
        showGroupDeleteMenu(event, groupId);
      }
      // 显示群聊删除菜单
      function showGroupDeleteMenu(event, groupId) {
        const groupName = groupData.groups[groupId]?.groupName || '未知群聊';
        ElMessageBox.confirm(
          `对群聊"${groupName}"执行什么操作？`,
          '群聊操作',
          {
            confirmButtonText: '从列表删除',
            cancelButtonText: '取消',
            type: 'warning',
          }
        ).then(() => {
          removeGroupFromList(groupId);
        }).catch(() => {
          // 用户取消操作
        });
      }
      // 初始化暗色模式（与博客前端保持一致）
      function initDarkMode() {
        try {
          // 首先检查body是否已经有dark-mode类（由路由守卫设置）
          const bodyHasDarkClass = document.body.classList.contains('dark-mode');
          const userTheme = localStorage.getItem('theme');
          if (userTheme === 'dark') {
            console.log('[聊天室] 恢复用户手动设置的暗色主题');
            data.isDarkMode = true;
            // 如果body还没有dark-mode类，则应用
            if (!bodyHasDarkClass) {
              applyDarkTheme();
            }
          } else if (userTheme === 'light') {
            console.log('[聊天室] 恢复用户手动设置的浅色主题');
            data.isDarkMode = false;
            // 如果body有dark-mode类，则移除
            if (bodyHasDarkClass) {
              applyLightTheme();
            }
          } else {
            // 用户未手动设置，检查系统偏好
            console.log('[聊天室] 未找到用户设置，检查系统暗色模式偏好');
            const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
            if (prefersDark) {
              console.log('[聊天室] 系统使用暗色模式');
              data.isDarkMode = true;
              if (!bodyHasDarkClass) {
                applyDarkTheme();
              }
            } else {
              console.log('[聊天室] 系统使用浅色模式');
              data.isDarkMode = false;
              if (bodyHasDarkClass) {
                applyLightTheme();
              }
            }
          }
        } catch (error) {
          console.error('[聊天室] 初始化暗色模式失败:', error);
        }
      }
      // 监听系统暗色模式变化（与博客前端保持一致）
      function setupSystemThemeListener() {
        if (!window.matchMedia) {
          console.log('[聊天室] 浏览器不支持matchMedia，跳过系统主题监听');
          return;
        }
        const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');
        const handleThemeChange = (e) => {
          console.log('[聊天室] 检测到系统主题变化:', e.matches ? '暗色' : '浅色');
          // 检查用户是否手动设置过主题
          const userTheme = localStorage.getItem('theme');
          // 只有在用户未手动设置时才自动切换
          if (!userTheme) {
            console.log('[聊天室] 用户未手动设置主题，跟随系统切换');
            if (e.matches) {
              data.isDarkMode = true;
              applyDarkTheme();
            } else {
              data.isDarkMode = false;
              applyLightTheme();
            }
          } else {
            console.log('[聊天室] 用户已手动设置主题为:', userTheme, '不自动跟随系统');
          }
        };
        // 添加监听器
        if (darkModeQuery.addEventListener) {
          darkModeQuery.addEventListener('change', handleThemeChange);
        } else if (darkModeQuery.addListener) {
          // 兼容旧版浏览器
          darkModeQuery.addListener(handleThemeChange);
        }
        console.log('[聊天室] 系统暗色模式监听已设置');
      }
      // 设置跨标签页主题同步（与博客前端保持一致）
      function setupStorageListener() {
        window.addEventListener('storage', (e) => {
          if (e.key === 'theme') {
            console.log('[聊天室] 检测到其他标签页主题变化:', e.newValue);
            const isDark = e.newValue === 'dark';
            data.isDarkMode = isDark;
            if (isDark) {
              applyDarkTheme();
            } else {
              applyLightTheme();
            }
          }
        });
        console.log('[聊天室] 跨标签页主题同步已设置');
      }
      // 应用暗色主题（与博客前端保持一致）
      function applyDarkTheme() {
        let root = document.querySelector(':root');
        document.body.classList.add('dark-mode');
        document.documentElement.classList.add('dark-mode');
        root.style.setProperty('--background', '#272727');
        root.style.setProperty('--fontColor', 'white');
        root.style.setProperty('--borderColor', '#4F4F4F');
        root.style.setProperty('--borderHoverColor', 'black');
        root.style.setProperty('--articleFontColor', '#E4E4E4');
        root.style.setProperty('--articleGreyFontColor', '#D4D4D4');
        root.style.setProperty('--commentContent', '#383838');
        root.style.setProperty('--favoriteBg', '#1e1e1e');
        root.style.setProperty('--whiteMask', 'rgba(56, 56, 56, 0.3)');
        root.style.setProperty('--maxWhiteMask', 'rgba(56, 56, 56, 0.5)');
        root.style.setProperty('--maxMaxWhiteMask', 'rgba(56, 56, 56, 0.7)');
        root.style.setProperty('--miniWhiteMask', 'rgba(56, 56, 56, 0.15)');
        root.style.setProperty('--mask', 'rgba(0, 0, 0, 0.5)');
        root.style.setProperty('--miniMask', 'rgba(0, 0, 0, 0.3)');
        root.style.setProperty('--inputBackground', '#383838');
        root.style.setProperty('--secondaryText', '#B0B0B0');
        root.style.setProperty('--card-bg-rgb', '39, 39, 39');
        // root.style.setProperty('--white', '#272727');
        // IM 聊天室专用变量
        root.style.setProperty('--maxWhite', '#3a3a3a');
        root.style.setProperty('--midWhite', '#303030');
        root.style.setProperty('--imBG', '#5d5d5d');
        root.style.setProperty('--messageColor', '#4a4a4a');
        root.style.setProperty('--maxLightGray', '#4F4F4F');
        root.style.setProperty('--lightGray', '#3a3a3a');
        root.style.setProperty('--blue', '#5090ff');
        root.style.setProperty('--greyFont', '#B0B0B0');
        console.log('[聊天室] 已应用暗色主题');
      }
      // 应用亮色主题（与博客前端保持一致）
      function applyLightTheme() {
        let root = document.querySelector(':root');
        document.body.classList.remove('dark-mode');
        document.documentElement.classList.remove('dark-mode');
        root.style.setProperty('--background', 'white');
        root.style.setProperty('--fontColor', 'black');
        root.style.setProperty('--borderColor', 'rgba(0, 0, 0, 0.5)');
        root.style.setProperty('--borderHoverColor', 'rgba(110, 110, 110, 0.4)');
        root.style.setProperty('--articleFontColor', '#1F1F1F');
        root.style.setProperty('--articleGreyFontColor', '#616161');
        root.style.setProperty('--commentContent', '#F7F9FE');
        root.style.setProperty('--favoriteBg', '#f7f9fe');
        root.style.setProperty('--whiteMask', 'rgba(255, 255, 255, 0.3)');
        root.style.setProperty('--maxWhiteMask', 'rgba(255, 255, 255, 0.5)');
        root.style.setProperty('--maxMaxWhiteMask', 'rgba(255, 255, 255, 0.7)');
        root.style.setProperty('--miniWhiteMask', 'rgba(255, 255, 255, 0.15)');
        root.style.setProperty('--mask', 'rgba(0, 0, 0, 0.3)');
        root.style.setProperty('--miniMask', 'rgba(0, 0, 0, 0.15)');
        root.style.setProperty('--inputBackground', '#f5f5f5');
        root.style.setProperty('--secondaryText', '#666666');
        root.style.setProperty('--card-bg-rgb', '255, 255, 255');
        root.style.setProperty('--white', 'white');
        // IM 聊天室专用变量
        root.style.setProperty('--maxWhite', '#f5f5f5');
        root.style.setProperty('--midWhite', '#fafafa');
        root.style.setProperty('--imBG', '#e8e8e8');
        root.style.setProperty('--messageColor', '#95EC69');
        root.style.setProperty('--maxLightGray', 'rgba(0, 0, 0, 0.1)');
        root.style.setProperty('--lightGray', 'rgba(0, 0, 0, 0.05)');
        root.style.setProperty('--blue', '#409eff');
        root.style.setProperty('--greyFont', '#666666');
        console.log('[聊天室] 已应用亮色主题');
      }
      // 暗色模式切换（与博客前端保持一致）
      function toggleDarkMode() {
        data.isDarkMode = !data.isDarkMode;
        // 保存到与博客前端共享的theme键
        localStorage.setItem('theme', data.isDarkMode ? 'dark' : 'light');
        // 应用主题
        if (data.isDarkMode) {
          applyDarkTheme();
        } else {
          applyLightTheme();
        }
        console.log('[聊天室] 主题已切换为:', data.isDarkMode ? '暗色' : '浅色');
      }
      // 初始化暗色模式（与博客前端保持一致）- 在函数定义之后调用
      initDarkMode();
      // 设置系统主题监听（与博客前端保持一致）
      setupSystemThemeListener();
      // 设置跨标签页主题同步（与博客前端保持一致）
      setupStorageListener();
      
      // 组件挂载完成后设置isMounted为true，启用teleport
      // 这解决了teleport目标元素在挂载时可能尚未存在的时序问题
      let minuteTickTimer = null;

      // 窗口大小变化处理
      const handleWindowResize = () => {
        // 当从移动端切换到PC端时，确保body-left显示
        if (!isMobile() && !imUtilData.showBodyLeft) {
          imUtilData.showBodyLeft = true;
          mobileRight(true); // 跳过动画
          saveShowBodyLeftState();
        }
        // 当从PC端切换到移动端时，如果当前是右侧内容界面，则隐藏body-left
        if (isMobile() && (data.subType === 1 || data.subType === 2 || data.subType === 3 || data.subType === 4 || data.subType === 5) && imUtilData.showBodyLeft) {
          imUtilData.showBodyLeft = false;
          mobileRight(true); // 跳过动画
          saveShowBodyLeftState();
        }
      };
      onMounted(() => {
        // 桌面浏览器监听缩放
        window.addEventListener('resize', handleWindowResize);
        data.isMounted = true;
        // 每分钟更新一次minuteTick，触发时间显示重新渲染
        minuteTickTimer = setInterval(() => {
          data.minuteTick++;
        }, 60000); // 每60秒更新一次
      });
      
      // 清理定时器和WebSocket连接
      onBeforeUnmount(() => {
        window.removeEventListener('resize', handleWindowResize);
        if (minuteTickTimer) {
          clearInterval(minuteTickTimer);
          minuteTickTimer = null;
        }
        
        // 清理在线人数定时器
        if (data.onlineCountTimer) {
          clearTimeout(data.onlineCountTimer);
          data.onlineCountTimer = null;
        }

        // 彻底销毁IM连接及其资源
        if (im) {
          console.log('[聊天室] 正在断开IM WebSocket连接...');
          im.destroy();
          im = null;
        }
      });
      
      // 格式化时间方法，接受tick参数强制Vue在tick变化时重新计算
      const formatTimeWithTick = (dateStr, tick) => {
        // tick参数仅用于触发重新计算，不参与实际逻辑
        void tick;
        return formatChatTime(dateStr);
      };
      
      return {
        ...toRefs(data),
        ...toRefs(bindEmailData),
        ...toRefs(friendCircleData),
        ...toRefs(friendData),
        ...toRefs(groupData),
        ...toRefs(imUtilData),
        ...toRefs(changeDataData),
        // 聊天数据computed属性
        imMessages,
        imMessageBadge,
        imChats,
        groupMessages,
        groupMessageBadge,
        groupChats,
        lastMessagePreviews,
        // store 兼容层 - 让模板中的 $store.state 可以工作
        $store: store,
        // 时间格式化方法（支持自动刷新）
        formatTimeWithTick,
        // 工具方法
        isMobile,
        // 方法
        isActive,
        sendMsg,
        submitAvatar,
        changeFriendStatus,
        submitChange,
        removeFriend,
        sendFriendMessage,
        changeDataType,
        changeAvatar,
        sendGroupMessage,
        changeAside,
        openFriendCircle,
        launch,
        deleteTreeHole,
        submitWeiYan,
        pageWeiYan,
        cleanFriendCircle,
        addFriend,
        getCode,
        submitDialog,
        exitGroup,
        dissolveGroup,
        removeGroupFromList,
        getMessagePreview,
        showChatList,
        handleGroupChatClick,
        handleTouchStart,
        handleTouchEnd,
        handleContextMenu,
        showGroupDeleteMenu,
        toggleDarkMode
      }
    }
  }
</script>
<style scoped>
  .friend-wrap {
    background: var(--imBackground) center center / cover no-repeat;
    width: 100vw;
    height: 100vh;
  }
  .friend-box {
    display: flex;
    background-color: var(--background);
    border-radius: 10px;
    position: absolute;
    width: 70vw;
    max-width: 1000px;
    height: 80vh;
    top: 50%;
    left: 50%;
    transform: translateX(-50%) translateY(-50%);
    overflow: hidden;
  }
  .friend-aside {
    width: 60px;
    border-right: 1px solid var(--maxLightGray);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    /* 移动端顶部安全区域 */
    padding-top: env(safe-area-inset-top);
    box-sizing: border-box;
  }
  .friend-bode {
    flex: 1;
    display: flex;
  }
  .body-left {
    width: 260px;
    box-shadow: 3px 0 6px var(--maxLightGray);
    /* 移动端顶部安全区域 */
    padding-top: env(safe-area-inset-top);
    box-sizing: border-box;
  }
  .body-right {
    flex: 1;
    position: relative;
  }
  .user-thumb {
    width: 36px;
    height: 36px;
    /* 移动端添加顶部安全区域间距 */
    margin-top: max(15px, calc(10px + env(safe-area-inset-top)));
    margin-left: 12px;
    cursor: pointer;
  }
  .friend-chat {
    /* 移动端第一个聊天按钮考虑安全区域 */
    margin-top: 25px;
    text-align: center;
    cursor: pointer;
  }
  /* 移动端侧边栏第一个按钮顶部间距（friend-aside已有padding-top处理安全区域，这里只需基础间距） */
  @media screen and (max-width: 850px) {
    .friend-aside > div:first-child > div:nth-child(2) {
      margin-top: 25px;
    }
  }
  .friend-text {
    font-size: 13px;
  }
  .friend-set {
    text-align: center;
    margin-bottom: 20px;
    /* 移动端底部安全区域 */
    padding-bottom: env(safe-area-inset-bottom);
    cursor: pointer;
  }
  .im-input {
    margin: 13px;
    width: 240px;
  }
  /* 移动端搜索框额外顶部间距 - 避免被地址栏遮挡 */
  @media screen and (max-width: 850px) {
    .body-left > div:first-child {
      padding-top: max(10px, env(safe-area-inset-top));
    }
  }
  .im-user:hover, .im-user-group:hover {
    background-color: var(--maxLightGray);
  }
  .im-user {
    display: flex;
    align-items: center;
    padding: 10px;
    cursor: pointer;
    height: 70px;
    box-sizing: border-box;
    position: relative;
    transition: background-color 0.2s ease;
    color: var(--fontColor);
  }
  /* 删除按钮样式 */
  .chat-item-delete {
    position: absolute;
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background-color: rgba(255, 71, 87, 0.1);
    opacity: 0;
    transition: all 0.2s ease;
    cursor: pointer;
  }
  .chat-item-delete:hover {
    background-color: rgba(255, 71, 87, 0.2);
    transform: translateY(-50%) scale(1.1);
  }
  .im-user:hover .chat-item-delete {
    opacity: 1;
  }
  /* 🆕 移动端长按反馈样式 */
  @media (max-width: 768px) {
    .im-user {
      -webkit-user-select: none;
      -moz-user-select: none;
      -ms-user-select: none;
      user-select: none;
      -webkit-touch-callout: none;
    }
    .im-user:active {
      background-color: rgba(0, 0, 0, 0.05);
      transform: scale(0.98);
    }
    /* 移动端不显示删除按钮，改用长按 */
    .chat-item-delete {
      display: none;
    }
  }
  /* PC端右键菜单提示 - 只在聊天列表中显示 */
  @media (min-width: 769px) {
    .im-user {
      position: relative;
    }
    /* 只对聊天列表项显示"右键删除"提示 */
    .im-user-current::after,
    .im-group-current::after {
      content: '右键删除';
      position: absolute;
      right: 10px;
      top: 50%;
      transform: translateY(-50%);
      font-size: 12px;
      color: var(--greyFont);
      opacity: 0;
      transition: opacity 0.2s ease;
      pointer-events: none;
    }
    .im-user-current:hover::after,
    .im-group-current:hover::after {
      opacity: 0.6;
    }
    .im-user-current:hover .chat-item-delete ~ ::after,
    .im-group-current:hover .chat-item-delete ~ ::after {
      opacity: 0;
    }
  }
  .im-user-group {
    display: flex;
    align-items: center;
    padding: 10px;
    cursor: pointer;
    height: 70px;
    box-sizing: border-box;
  }
  .im-active, .friend-active, .im-group {
    background: var(--imBG);
  }
  .im-active .im-user-name,
  .im-active .im-message-time,
  .im-active .im-down,
  .friend-active .im-user-name,
  .friend-active .im-message-time,
  .friend-active .im-down,
  .im-group .im-user-name {
    color: var(--fontColor) !important;
  }
  .im-user-right {
    flex: 1;
    margin-left: 20px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
  }
  .im-user-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2px;
  }
  .im-user-name {
    font-weight: 500;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .im-message-time {
    font-size: 12px;
    color: var(--greyFont);
    margin-left: 10px;
    flex-shrink: 0;
    white-space: nowrap;
  }
  .aside-list {
    height: calc(100% - 60px);
  }
  .im-down {
    margin-top: 2px;
    font-size: 12px;
    color: var(--greyFont);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .aside-active {
    color: var(--blue);
    font-weight: 800;
    position: relative;
  }
  .aside-active::after {
    content: '';
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 20px;
    background: var(--blue);
    border-radius: 2px;
  }
  .im-friend {
    margin-left: 20px;
    align-self: center;
    font-size: 15px;
  }
  .body-left :deep(.n-divider .n-divider__title) {
    color: var(--greyFont);
    font-size: 12px;
    letter-spacing: 2px;
  }
  .group-tag {
    border-radius: 2px;
    padding: 2px;
    background: var(--themeBackground);
    color: var(--background);
    margin-right: 12px;
  }
  .msg-one {
    margin: 15px 20px;
    display: flex;
    align-items: flex-start;
  }
  /* 滚动条 */
  .friend-box ::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  .friend-box ::-webkit-scrollbar-thumb {
    border-radius: 5px;
    background-color: var(--lowGray);
  }
  .sendMsg .n-button {
    height: 35px;
    padding: 15px 25px;
  }
  .system-date {
    font-size: 12px;
    color: var(--greyFont);
    margin-bottom: 5px;
  }
  .system-content {
    background-color: var(--messageColor);
    border-radius: 4px;
    max-width: 80%;
    padding: 5px 10px;
    line-height: 25px;
    word-break: break-all;
    color: var(--black);
  }
  .friend-request {
    background: var(--midWhite);
    overflow-y: auto;
    height: calc(100% - 60px);
  }
  .request-status {
    font-size: 16px;
    color: var(--greyFont);
    flex: 1;
    display: flex;
    justify-content: right;
    align-items: center;
  }
  .friend-label {
    color: var(--greyFont);
    margin-right: 30px;
  }
  .friend-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 65%;
    padding-top: 50px;
  }
  .body-right .n-card.n-card--bordered {
    border: none;
  }
  #outerImg {
    position: fixed;
    top: 0;
    left: 0;
    background: rgba(0, 0, 0, 0.6);
    z-index: 10;
    width: 100%;
    height: 100%;
    display: none;
  }
  .mobile-right::before {
    content: "👈";
    position: absolute;
    width: 100%;
    height: 100%;
    font-size: 40px;
    color: var(--background);
    top: 0;
    left: 0;
    background: var(--maxGreyFont);
    z-index: 100;
    display: flex;
    align-items: center;
    cursor: pointer;
  }
  .chat-container {
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  .chat-container > * {
    display: flex;
    flex-direction: column;
  }
  .pagination-wrap {
    display: flex;
    justify-content: center;
    height: 35px;
    margin-top: 15px;
    margin-bottom: 10px;
  }
  .treeHole-wrap {
    width: 800px;
    height: 600px;
    overflow-y: auto;
    padding: 0; /* 确保内容区无额外间距 */
  }
  .empty-content {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    color: var(--greyFont);
    padding: 0;
    margin: 0;
  }
  .treeHole-wrap::-webkit-scrollbar-thumb {
    background-color: var(--lowGray);
    border-radius: 5px;
  }
  .treeHole-wrap::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  .weiyan-edit {
    width: 350px;
    background-color: var(--background);
    padding: 20px;
  }
  .email-wrap {
    padding: 20px;
    background: var(--background);
    border-radius: 10px;
    transition: all 1s ease;
  }
  .email-title {
    line-height: 30px;
    font-size: 24px;
    margin-bottom: 15px;
  }
  @media screen and (max-width: 1200px) {
    .friend-box {
      width: 95vw;
      height: 95vh;
    }
  }
  @media screen and (max-width: 850px) {
    .friend-box {
      width: 100vw;
      /* 先使用 vh 作为备用值，再使用 dvh (动态视口高度) 避免地址栏遮挡 */
      height: 100vh;
      height: 100dvh;
      border-radius: unset;
      /* 移动端不在外层容器添加padding，避免露出背景 */
      /* 安全区域由内部子元素（friend-aside、body-left、body-right）自己处理 */
      padding: 0;
      box-sizing: border-box;
      position: relative;
    }
    .friend-box .el-overlay {
      position: absolute !important;
    }
    /* 移动端侧边栏占满整个高度 */
    .friend-aside {
      height: 100%;
    }
    .friend-bode {
      max-width: calc(100vw - 60px);
      position: relative;
      overflow: hidden;
      height: 100%;
    }
    .body-left {
      position: absolute;
      top: 0;
      left: 0;
      height: 100%;
      /* 移动端宽度占满整个可用空间 */
      width: calc(100vw - 60px);
      z-index: 10;
      background: var(--background);
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
      /* 移动端左侧面板不需要额外的padding，因为已经在friend-box上处理了 */
      padding-top: 0;
    }
    .body-left.hidden {
      transform: translateX(-100%);
    }
    .body-right {
      width: 100%;
      max-width: calc(100vw - 60px);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      background: var(--background);
      /* 移动端添加顶部安全区域 */
      padding-top: env(safe-area-inset-top);
      box-sizing: border-box;
      /* 当左侧面板显示时，右侧内容完全隐藏 */
      position: absolute;
      left: 0;
    }
    .body-right.full-width {
      margin-left: 0;
    }
    /* 移动端搜索框宽度调整 */
    .im-input {
      width: calc(100% - 26px);
    }
    .treeHole-wrap {
      width: calc(100vw - 40px);
    }
    .mobile-back-btn {
      display: flex !important;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      margin-right: 10px;
      cursor: pointer;
      border-radius: 50%;
      transition: all 0.2s ease;
      color: var(--blue);
      animation: slideInFromLeft 0.3s ease-out;
    }
    .mobile-back-btn:hover {
      background-color: var(--lightGray);
    }
    .mobile-back-btn:active {
      transform: scale(0.95);
      background-color: rgba(0, 0, 0, 0.05);
    }
    /* 移动端动画效果 */
    @keyframes slideInFromLeft {
      from {
        opacity: 0;
        transform: translateX(-20px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }
    @keyframes slideInFromRight {
      from {
        opacity: 0;
        transform: translateX(20px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }
    /* 侧边栏按钮点击效果 */
    .friend-chat {
      transition: all 0.2s ease;
      position: relative;
    }
    .friend-chat:active {
      transform: scale(0.95);
      background-color: rgba(0, 0, 0, 0.05);
    }
    .friend-chat.aside-active::after {
      content: '';
      position: absolute;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 20px;
      background: var(--blue);
      border-radius: 2px;
      animation: slideInFromRight 0.3s ease-out;
    }
    /* 列表项点击效果 */
    .im-user, .im-user-group {
      transition: all 0.2s ease;
    }
    .im-user:active, .im-user-group:active {
      transform: scale(0.98);
      background-color: rgba(0, 0, 0, 0.03);
    }
    /* 聊天界面进入动画 */
    .chat-container {
      animation: slideInFromRight 0.3s ease-out;
    }
    /* 优化滚动性能 */
    .aside-list {
      -webkit-overflow-scrolling: touch;
      scroll-behavior: smooth;
    }
    .msg-container {
      -webkit-overflow-scrolling: touch;
      scroll-behavior: smooth;
    }
  }
  @media screen and (max-width: 400px) {
    .msg-one {
      margin: 15px 10px;
    }
    .friend-model {
      width: 90% !important;
    }
  }
  /* 系统消息特殊样式 - 禁用删除功能 */
  .system-message::after {
    display: none !important; /* 不显示"右键删除"提示 */
  }
  .system-message .chat-item-delete {
    display: none !important; /* 不显示删除按钮 */
  }
  /* 系统消息禁用右键菜单和长按 */
  .system-message {
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
  }
  /* 系统消息特殊样式 - 禁用删除功能 */
  .system-message::after {
    display: none !important; /* 不显示"右键删除"提示 */
  }
  .system-message .chat-item-delete {
    display: none !important; /* 不显示删除按钮 */
  }
  /* 系统消息禁用右键菜单和长按选择 */
  .system-message {
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
  }
  /* 移动端群聊项优化 - 防止右键菜单和文本选择 */
  @media (max-width: 768px) {
    .im-group-current {
      -webkit-user-select: none;
      -moz-user-select: none;
      -ms-user-select: none;
      user-select: none;
      -webkit-touch-callout: none; /* 禁用iOS长按弹出菜单 */
      -webkit-tap-highlight-color: transparent; /* 移除点击高亮 */
    }
    /* 移动端禁用右键菜单 */
    .im-group-current {
      pointer-events: auto;
    }
    .im-group-current * {
      -webkit-user-select: none;
      -moz-user-select: none;
      -ms-user-select: none;
      user-select: none;
    }
  }
</style>
<!-- 全局样式 - 用于 Element Plus 组件和 CSS 变量 -->
<style>
  /* friend-model dialog 样式 */
  .friend-model.el-dialog {
    background-color: transparent;
    box-shadow: none;
    margin-top: 5vh !important; /* 强制覆盖默认margin */
  }
  
  /* 彻底隐藏被挂载到 body 的所有 friend-model 对话框的 header */
  .friend-model .el-dialog__header {
    display: none !important;
  }
  
  /* 随笔对话框header样式 - 移除下边框 */
  .weiyan-dialog .el-dialog__header {
    border-bottom: none;
    padding-right: 40px; /* 给关闭按钮留空间 */
  }
  
  .friend-model .el-dialog__body {
    background-color: transparent;
    color: var(--fontColor);
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0;
  }
  
  /* 朋友圈对话框内容填满宽度但treeHole保持固定宽度 */
  .friend-circle-dialog .el-dialog__body {
    display: flex;
    justify-content: center;
  }
  
  /* 随笔对话框内容居中 */
  .weiyan-edit {
    width: 350px; /* 固定宽度 */
    background-color: var(--background);
    border-radius: 0 0 10px 10px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  
  .weiyan-edit .comment-textarea {
    width: 100%;
  }
  
  .weiyan-edit .myBetween {
    width: 100%;
  }
  
  /* 聊天输入框样式 - 确保应用到 textarea */
  .im-message-content {
    background-color: var(--background) !important;
    color: var(--fontColor) !important;
  }
  
  /* el-input 样式 */
  .im-input .el-input__wrapper {
    background-color: var(--background);
  }
  
  .im-input .el-input__inner {
    color: var(--fontColor);
  }
  
  /* Dialog Containment: Force overlay to be absolute within friend-wrap */
  .friend-wrap .el-overlay {
    position: absolute !important;
  }
</style>
