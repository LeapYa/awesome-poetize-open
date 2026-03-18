<template>
  <div>
    <div v-if="!$common.isEmpty(article)">
      <!-- 封面 -->
      <div class="article-head my-animation-slide-top">
        <!-- 背景图片 -->
        <el-image
          class="article-image my-el-image"
          v-once
          lazy
          :src="article.articleCover"
          fit="cover"
        >
          <template v-slot:error>
            <div
              class="image-slot"
              style="background-color: var(--lightGreen)"
            ></div>
          </template>
        </el-image>

        <!-- 语言切换按钮容器 -->
        <div class="language-switch-container">
          <!-- 动态语言切换按钮 -->
          <div
            class="article-language-switch"
            v-if="availableLanguageButtons.length > 1"
          >
            <el-button-group>
              <el-button
                v-for="langButton in availableLanguageButtons"
                :key="langButton.code"
                :ref="`langBtn_${langButton.code}`"
                size="mini"
                :type="currentLang === langButton.code ? 'primary' : 'default'"
                @click.stop="handleLanguageSwitch(langButton.code)"
                @mousedown.stop="handleMouseDown"
                @touchstart.stop="handleTouchStart"
                :title="`切换到${langButton.name}`"
                :data-lang="langButton.code"
              >
                {{ langButton.name }}
              </el-button>
            </el-button-group>
          </div>
        </div>
        <!-- 文章信息 -->
        <div class="article-info-container">
          <div class="article-title">{{ articleTitle }}</div>
          <div class="article-info">
            <svg
              viewBox="0 0 1024 1024"
              width="14"
              height="14"
              style="vertical-align: -2px"
            >
              <path
                d="M510.4 65.5l259.69999999 0 1e-8 266.89999999c0 147.50000001-116.2 266.89999999-259.7 266.90000001-143.4 0-259.7-119.5-259.7-266.90000001 0.1-147.5 116.3-266.9 259.7-266.89999999z"
                fill="#FF9FCF"
              ></path>
              <path
                d="M698.4 525.2l-13 0c53-48.4 86.5-117.8 86.5-195.20000001 0-10.2-0.7-20.3-1.8-30.19999999C613.8 377.50000001 438.6 444.9 266 437.7c15 33.4 36.7 63.1 63.5 87.5l-5.3 0c-122.6 0-225.5 88.1-248.8 204.1C340 677.2 597.7 609.2 862.2 585.7c-44.3-37.6-101.5-60.5-163.8-60.5z"
                fill="#FF83BB"
              ></path>
              <path
                d="M862.2 585.7C597.7 609.2 340 677.2 75.4 729.3c-3.2 16.1-5 32.6-5 49.6 0 99.8 81.7 181.5 181.5 181.5l518.6 0c99.8 0 181.5-81.7 181.5-181.5 0.1-77.2-35-146.5-89.8-193.2z"
                fill="#FF5390"
              ></path>
              <path
                d="M770.1 299.8C755.1 168 643.3 65.5 507.4 65.5c-146.1 0-264.5 118.4-264.5 264.5 0 38.4 8.3 74.8 23.1 107.7 172.6 7.2 347.8-60.2 504.1-137.9z"
                fill="#FF9FCF"
              ></path>
              <path
                d="M436.4 282.1c0 24.1-19.6 43.7-43.7 43.7S349 306.2 349 282.1s19.6-43.7 43.7-43.7c24.19999999 0 43.7 19.6 43.7 43.7z"
                fill="#FFFFFF"
              ></path>
              <path
                d="M625 282.1m-43.7 0a43.7 43.7 0 1 0 87.4 0 43.7 43.7 0 1 0-87.4 0Z"
                fill="#FFFFFF"
              ></path>
            </svg>
            <span>&nbsp;{{ article.username }}</span>
            <span>·</span>
            <svg
              viewBox="0 0 1024 1024"
              width="14"
              height="14"
              style="vertical-align: -2px"
            >
              <path
                d="M512 512m-512 0a512 512 0 1 0 1024 0 512 512 0 1 0-1024 0Z"
                fill="#409EFF"
              ></path>
              <path
                d="M654.222222 256c-17.066667 0-28.444444 11.377778-28.444444 28.444444v56.888889c0 17.066667 11.377778 28.444444 28.444444 28.444445s28.444444-11.377778 28.444445-28.444445v-56.888889c0-17.066667-11.377778-28.444444-28.444445-28.444444zM369.777778 256c-17.066667 0-28.444444 11.377778-28.444445 28.444444v56.888889c0 17.066667 11.377778 28.444444 28.444445 28.444445s28.444444-11.377778 28.444444-28.444445v-56.888889c0-17.066667-11.377778-28.444444-28.444444-28.444444z"
                fill="#FFFFFF"
              ></path>
              <path
                d="M725.333333 312.888889H711.111111v28.444444c0 31.288889-25.6 56.888889-56.888889 56.888889s-56.888889-25.6-56.888889-56.888889v-28.444444h-170.666666v28.444444c0 31.288889-25.6 56.888889-56.888889 56.888889s-56.888889-25.6-56.888889-56.888889v-28.444444h-14.222222c-22.755556 0-42.666667 19.911111-42.666667 42.666667v341.333333c0 22.755556 19.911111 42.666667 42.666667 42.666667h426.666666c22.755556 0 42.666667-19.911111 42.666667-42.666667v-341.333333c0-22.755556-19.911111-42.666667-42.666667-42.666667zM426.666667 654.222222h-56.888889c-17.066667 0-28.444444-11.377778-28.444445-28.444444s11.377778-28.444444 28.444445-28.444445h56.888889c17.066667 0 28.444444 11.377778 28.444444 28.444445s-11.377778 28.444444-28.444444 28.444444z m227.555555 0h-56.888889c-17.066667 0-28.444444-11.377778-28.444444-28.444444s11.377778-28.444444 28.444444-28.444445h56.888889c17.066667 0 28.444444 11.377778 28.444445 28.444445s-11.377778 28.444444-28.444445 28.444444z m0-113.777778h-56.888889c-17.066667 0-28.444444-11.377778-28.444444-28.444444s11.377778-28.444444 28.444444-28.444444h56.888889c17.066667 0 28.444444 11.377778 28.444445 28.444444s-11.377778 28.444444-28.444445 28.444444z"
                fill="#FFFFFF"
              ></path>
            </svg>
            <span>&nbsp;{{ article.createTime }}</span>
            <span>·</span>
            <svg
              viewBox="0 0 1024 1024"
              width="14"
              height="14"
              style="vertical-align: -2px"
            >
              <path
                d="M14.656 512a497.344 497.344 0 1 0 994.688 0 497.344 497.344 0 1 0-994.688 0z"
                fill="#FF0000"
              ></path>
              <path
                d="M374.976 872.64c-48.299-100.032-22.592-157.44 14.421-211.37 40.448-58.966 51.115-117.611 51.115-117.611s31.659 41.386 19.115 106.005c56.149-62.72 66.816-162.133 58.325-200.405 127.317 88.746 181.59 281.002 108.181 423.381C1016 652.501 723.093 323.2 672.277 285.867c16.939 37.333 20.054 100.032-14.101 130.474-58.027-219.84-201.664-265.002-201.664-265.002 16.96 113.536-61.781 237.397-137.344 330.24-2.816-45.163-5.632-76.544-29.483-119.808-5.333 82.176-68.373 149.269-85.29 231.445-22.912 111.637 17.237 193.173 170.581 279.424z"
                fill="#FFFFFF"
              ></path>
            </svg>
            <span>&nbsp;{{ article.viewCount }}</span>
            <span>·</span>
            <svg
              viewBox="0 0 1024 1024"
              width="14"
              height="14"
              style="vertical-align: -2px"
            >
              <path
                d="M113.834667 291.84v449.194667a29.013333 29.013333 0 0 0 28.842666 29.013333h252.928v90.453333l160.597334-90.453333h252.928a29.013333 29.013333 0 0 0 29.013333-29.013333V291.84a29.013333 29.013333 0 0 0-29.013333-29.013333h-665.6a29.013333 29.013333 0 0 0-29.696 29.013333z"
                fill="#FFDEAD"
              ></path>
              <path
                d="M809.130667 262.826667h-665.6a29.013333 29.013333 0 0 0-28.842667 29.013333v40.106667a29.013333 29.013333 0 0 1 28.842667-29.013334h665.6a29.013333 29.013333 0 0 1 29.013333 29.013334V291.84a29.013333 29.013333 0 0 0-29.013333-29.013333z"
                fill="#FFF3DB"
              ></path>
              <path
                d="M556.202667 770.048h252.928a29.013333 29.013333 0 0 0 29.013333-29.013333V362.837333s-59.733333 392.533333-724.309333 314.709334v63.488a29.013333 29.013333 0 0 0 28.842666 29.013333h253.098667v90.453333z"
                fill="#F2C182"
              ></path>
              <path
                d="M619.008 632.32l101.888-35.157333-131.754667-76.117334 29.866667 111.274667zM891.904 148.992a61.44 61.44 0 0 0-84.138667 22.528l-19.968 34.133333 106.666667 61.610667 19.968-34.133333a61.781333 61.781333 0 0 0-22.528-84.138667z"
                fill="#69BAF9"
              ></path>
              <path
                d="M775.338667 198.775467l131.669333 76.032-186.026667 322.218666-131.6864-76.032z"
                fill="#F7FBFF"
              ></path>
              <path
                d="M775.168 198.826667l-5.290667 9.216 59.221334 34.133333a34.133333 34.133333 0 0 1 12.458666 46.592l-139.946666 242.346667a34.133333 34.133333 0 0 1-46.762667 12.629333l-59.050667-34.133333-6.656 11.434666 88.746667 51.2L720.896 597.333333l186.026667-322.56z"
                fill="#D8E3F0"
              ></path>
              <path
                d="M616.448 622.592l2.56 9.728 101.888-35.157333-44.885333-25.941334-59.562667 51.370667zM891.904 148.992c-1.024 0-2.218667-0.853333-3.242667-1.536A61.610667 61.610667 0 0 1 887.466667 204.8l-19.968 34.133333-73.728-42.496-5.12 8.704 106.666666 61.610667 19.968-34.133333a61.781333 61.781333 0 0 0-23.381333-83.626667z"
                fill="#599ED4"
              ></path>
              <path
                d="M265.898667 417.621333H494.933333a17.066667 17.066667 0 1 0 0-34.133333H265.898667a17.066667 17.066667 0 1 0 0 34.133333zM265.898667 533.504H494.933333a17.066667 17.066667 0 0 0 0-34.133333H265.898667a17.066667 17.066667 0 0 0 0 34.133333z"
                fill="#3D3D63"
              ></path>
              <path
                d="M959.488 354.645333a99.84 99.84 0 0 0-23.722667-127.488 78.677333 78.677333 0 0 0-142.848-64.170666l-11.605333 20.138666a17.066667 17.066667 0 0 0-20.821333 7.168l-32.085334 55.466667H142.677333a46.250667 46.250667 0 0 0-45.909333 46.08v449.194667a46.08 46.08 0 0 0 45.909333 46.08h236.032v73.386666a17.066667 17.066667 0 0 0 8.362667 14.848 17.066667 17.066667 0 0 0 8.704 2.218667 17.066667 17.066667 0 0 0 8.362667-2.218667l156.672-88.234666h248.32a46.08 46.08 0 0 0 46.08-46.08V398.677333L921.6 283.306667a17.066667 17.066667 0 0 0-4.266667-21.504l1.877334-3.413334a65.365333 65.365333 0 0 1 10.410666 79.189334l-53.077333 91.989333a56.832 56.832 0 0 0 20.821333 77.653333 17.066667 17.066667 0 0 0 24.234667-6.314666 17.066667 17.066667 0 0 0-6.997333-23.04 23.04 23.04 0 0 1-8.362667-31.061334z m-138.410667 386.389334a11.946667 11.946667 0 0 1-11.946666 11.946666H556.202667a17.066667 17.066667 0 0 0-8.362667 2.218667l-134.997333 76.117333v-61.269333a17.066667 17.066667 0 0 0-17.066667-17.066667H142.677333a11.946667 11.946667 0 0 1-11.776-11.946666V291.84a11.946667 11.946667 0 0 1 11.776-11.946667h565.930667L574.464 512a17.066667 17.066667 0 0 0-1.706667 12.970667L597.333333 615.253333H265.898667a17.066667 17.066667 0 1 0 0 34.133334h352.938666a17.066667 17.066667 0 0 0 5.802667 0l102.4-35.328a17.066667 17.066667 0 0 0 9.216-7.509334l85.333333-147.968z m-204.8-184.661334l63.829334 36.864-49.322667 17.066667z m206.848-170.666666v1.365333l-108.373333 186.709333-102.4-59.050666L781.482667 221.866667l102.4 59.050666z m76.458667-161.28L887.466667 244.224l-76.970667-44.373333 11.264-19.797334a44.544 44.544 0 1 1 77.141333 44.544z"
                fill="#3D3D63"
              ></path>
            </svg>
            <span>&nbsp;{{ article.commentCount }}</span>
          </div>
        </div>

        <div
          class="article-info-news"
          @click="weiYanDialogVisible = true"
          v-if="
            !$common.isEmpty(mainStore.currentUser) &&
            mainStore.currentUser.id === article.userId
          "
        >
          <svg width="30" height="30" viewBox="0 0 1024 1024">
            <path d="M0 0h1024v1024H0V0z" fill="#202425" opacity=".01"></path>
            <path
              d="M989.866667 512c0 263.918933-213.947733 477.866667-477.866667 477.866667S34.133333 775.918933 34.133333 512 248.081067 34.133333 512 34.133333s477.866667 213.947733 477.866667 477.866667z"
              fill="#FF7744"
            ></path>
            <path
              d="M512 221.866667A51.2 51.2 0 0 1 563.2 273.066667v187.733333H750.933333a51.2 51.2 0 0 1 0 102.4h-187.733333V750.933333a51.2 51.2 0 0 1-102.4 0v-187.733333H273.066667a51.2 51.2 0 0 1 0-102.4h187.733333V273.066667A51.2 51.2 0 0 1 512 221.866667z"
              fill="#FFFFFF"
            ></path>
          </svg>
        </div>
      </div>
      <!-- 文章 -->
      <div style="background: var(--background)">
        <div class="article-container my-animation-slide-bottom">
          <div
            v-if="!$common.isEmpty(article.videoUrl) && decryptedVideoUrl"
            style="margin-bottom: 20px"
          >
            <videoPlayer
              :url="{ src: decryptedVideoUrl }"
              :cover="article.articleCover"
            >
            </videoPlayer>
          </div>

          <!-- 最新进展 -->
          <div v-if="!$common.isEmpty(treeHoleList)" class="process-wrap">
            <el-collapse accordion model-value="1">
              <el-collapse-item title="最新进展" name="1">
                <process
                  :treeHoleList="treeHoleList"
                  @deleteTreeHole="deleteTreeHole"
                ></process>
              </el-collapse-item>
            </el-collapse>

            <hr />
          </div>

          <!-- 加载骨架 -->
          <div v-if="isLoading" class="entry-content">
            <el-skeleton :rows="10" animated />
          </div>
          <!-- 正文显示 -->
          <div
            v-else
            :key="articleContentKey"
            v-html="articleContentHtml"
            class="entry-content"
            :lang="currentLang"
          ></div>

          <!-- 付费墙提示 -->
          <div v-if="article.paywalled" class="paywall-block">
            <div class="paywall-fade"></div>
            <div class="paywall-card">
              <div class="paywall-badge">
                <span v-if="article.payType === 2">VIP专享</span>
                <span v-else>付费解锁</span>
              </div>
              <div class="paywall-hidden-prompt">
                <svg viewBox="0 0 1024 1024" width="16" height="16" style="vertical-align: -2px; margin-right: 6px;">
                  <path d="M512 42.666667A469.333333 469.333333 0 1 0 981.333333 512 469.333333 469.333333 0 0 0 512 42.666667z m0 768a53.333333 53.333333 0 1 1 53.333333-53.333334 53.333333 53.333333 0 0 1-53.333333 53.333334z m53.333333-234.666667a53.333333 53.333333 0 0 1-106.666666 0v-256a53.333333 53.333333 0 0 1 106.666666 0z" fill="#ff4d79"></path>
                </svg>
                此处内容已隐藏
              </div>
              <div class="paywall-desc" v-if="article.payType === 1">
                支付 <strong>¥{{ article.payAmount }}</strong> 即可解锁全文
              </div>
              <div class="paywall-desc" v-else-if="article.payType === 2">
                本文为会员专属内容，成为会员即可阅读
              </div>
              <div class="paywall-desc" v-else-if="article.payType === 3">
                赞赏 <strong>¥{{ article.payAmount }}</strong> 后可解锁全文
              </div>
              <div class="paywall-desc" v-else-if="article.payType === 4">
                支付 <strong>¥{{ article.payAmount }}</strong> 即可解锁全文
              </div>
              <div class="paywall-actions">
                <el-button
                  type="primary"
                  size="large"
                  round
                  @click="handlePayment"
                  :loading="paymentLoading"
                >
                  {{ article.payType === 2 ? '成为会员' : '立即解锁' }}
                </el-button>
              </div>
              <div class="paywall-meta" v-if="article.paidCount > 0">
                已有 {{ article.paidCount }} 人解锁
              </div>
              <div class="paywall-verify">
                <a href="javascript:void(0)" @click="verifyPayment" :class="{ checking: verifyingPayment }">
                  {{ verifyingPayment ? '验证中...' : '已支付？点击刷新状态' }}
                </a>
              </div>
            </div>
          </div>

          <!-- 最后更新时间 -->
          <div class="article-update-time">
            <span>文章最后更新于 {{ article.updateTime }}</span>
          </div>
          <!-- 分类 -->
          <div class="article-sort">
            <span
              draggable="true"
              @dragstart="handleSortDragStart($event)"
              @click="
                $router.push(
                  '/sort/' + article.sortId + '?labelId=' + article.labelId
                )
              "
              >{{
                article.sort.sortName + ' · ' + article.label.labelName
              }}</span
            >
          </div>
          <!-- 作者信息 -->
          <blockquote>
            <div>作者：{{ article.username }}</div>
            <div>
              <span>版权&许可请详阅</span>
              <span
                style="color: #38f; cursor: pointer"
                @click="copyrightDialogVisible = true"
              >
                版权声明
              </span>
            </div>
          </blockquote>
          <!-- 订阅和分享按钮 -->
          <div class="myCenter" id="article-like">
            <div
              class="subscribe-button"
              :class="{ subscribed: subscribe }"
              @click="subscribeLabel()"
            >
              {{ subscribe ? '已订阅' : '订阅' }}
              <el-icon><el-icon-upload /></el-icon>
            </div>
            <div class="share-card-button" @click="openShareCardDialog()">
              卡片分享
              <el-icon><el-icon-share /></el-icon>
            </div>
          </div>

          <!-- 评论 -->
          <div v-if="article.commentStatus === true && enableComment">
            <div ref="commentSentinel" style="height: 1px"></div>
            <comment
              v-if="shouldLoadComments"
              :type="'article'"
              :source="article.id"
              :userId="article.userId"
            ></comment>
          </div>
        </div>

        <teleport to="body">
          <div id="toc" class="toc"></div>
        </teleport>
      </div>

      <div style="background: var(--background)">
        <myFooter></myFooter>
      </div>
    </div>

    <el-dialog
      title="版权声明"
      v-model="copyrightDialogVisible"
      width="80%"
      :append-to-body="true"
      class="article-copy centered-dialog"
      :close-on-click-modal="false"
      center
    >
      <div style="display: flex; align-items: center; flex-direction: column">
        <el-avatar
          shape="square"
          :size="35"
          :src="$common.getAvatarUrl(mainStore.webInfo.avatar)"
        >
          <img :src="$getDefaultAvatar()" />
        </el-avatar>
        <div class="copyright-container">
          <p>
            {{ mainStore.webInfo.webTitle || mainStore.webInfo.webName }}是指运行在{{
              $constant.host
            }}域名及相关子域名上的网站，本条款描述了{{
              mainStore.webInfo.webTitle || mainStore.webInfo.webName
            }}的网站版权声明：
          </p>
          <ul>
            <li>
              {{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}提供的所有文章、展示的图片素材等内容部分来源于互联网平台，仅供学习参考。如有侵犯您的版权，请联系{{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}负责人，{{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}承诺将在一个工作日内改正。
            </li>
            <li>
              {{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}不保证网站内容的全部准确性、安全性和完整性，请您在阅读、下载及使用过程中自行确认，{{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}亦不承担上述资源对您造成的任何形式的损失或伤害。
            </li>
            <li>
              未经{{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}允许，不得盗链、盗用本站内容和资源。
            </li>
            <li>
              {{ mainStore.webInfo.webTitle || mainStore.webInfo.webName }}旨在为广大用户提供更多的信息；{{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}不保证向用户提供的外部链接的准确性和完整性，该外部链接指向的不由本站实际控制的任何网页上的内容，{{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}对其合法性亦概不负责，亦不承担任何法律责任。
            </li>
            <li>
              {{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}中的文章/视频（包括转载文章/视频）的版权仅归原作者所有，若作者有版权声明或文章从其它网站转载而附带有原所有站的版权声明者，其版权归属以附带声明为准；文章仅代表作者本人的观点，与{{
                mainStore.webInfo.webTitle || mainStore.webInfo.webName
              }}立场无关。
            </li>
            <li>
              {{ mainStore.webInfo.webTitle || mainStore.webInfo.webName }}自行编写排版的文章均采用
              <a
                href="https://creativecommons.org/licenses/by-nc-sa/4.0/"
                style="color: #38f; text-decoration: none"
              >
                知识共享署名-非商业性使用-相同方式共享 4.0 国际许可协议
              </a>
            </li>
            <li>
              许可协议标识：
              <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">
                <img
                  alt="知识共享许可协议"
                  src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png"
                  style="margin-top: 5px"
                />
              </a>
            </li>
          </ul>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      title="微言"
      :modal="false"
      v-model="weiYanDialogVisible"
      width="40%"
      :append-to-body="true"
      :close-on-click-modal="false"
      destroy-on-close
      center
    >
      <div>
        <div style="margin-bottom: 5px">
          Date：
          <el-date-picker
            v-model="newsTime"
            type="datetime"
            placeholder="Select date and time"
          >
          </el-date-picker>
        </div>
        <commentBox :disableGraffiti="true" @submitComment="submitWeiYan">
        </commentBox>
      </div>
    </el-dialog>

    <!-- 微信 -->
    <el-dialog
      title="密码"
      :modal="false"
      v-model="showPasswordDialog"
      width="25%"
      :append-to-body="true"
      :close-on-click-modal="false"
      destroy-on-close
      center
    >
      <div>
        <div>
          <div class="password-content">{{ tips }}</div>
        </div>
        <div style="margin: 20px auto">
          <el-input maxlength="30" v-model="password"></el-input>
        </div>
        <div style="display: flex; justify-content: center">
          <proButton
            :info="'提交'"
            @click="submitPassword()"
            :before="$constant.before_color_2"
            :after="$constant.after_color_2"
          >
          </proButton>
        </div>
      </div>
    </el-dialog>

    <!-- 卡片分享弹窗 -->
    <el-dialog
      title="卡片分享"
      v-model="shareCardDialogVisible"
      width="500px"
      :append-to-body="true"
      class="share-card-dialog centered-dialog"
      center
    >
      <div class="share-card-container">
        <!-- 卡片预览 -->
        <div class="share-card-preview" ref="shareCard" id="shareCard">
          <!-- 作者头像 -->
          <div class="card-avatar-container">
            <img
              :src="$common.getAvatarUrl(article.avatar)"
              alt="作者头像"
              class="card-avatar"
            />
          </div>

          <!-- 日期 -->
          <div class="card-date">
            {{ formatDate(article.createTime) }}
          </div>

          <!-- 标题 -->
          <div class="card-title">
            {{ articleTitle }}
          </div>

          <!-- 封面图片 -->
          <div class="card-cover">
            <img :src="article.articleCover" alt="文章封面" />
          </div>

          <!-- 底部信息 -->
          <div class="card-footer">
            <!-- 作者名 -->
            <div class="card-author">
              {{ article.username }}
            </div>

            <!-- 分隔线 -->
            <hr class="card-divider" />

            <!-- 品牌标识和二维码 -->
            <div class="card-bottom">
              <div class="card-brand">
                {{ mainStore.webInfo.webTitle }}
              </div>
              <div class="card-qrcode" ref="qrcode"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <template v-slot:footer>
        <div class="dialog-footer">
          <el-button @click="shareCardDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="downloadShareCard()"
            >下载卡片</el-button
          >
        </div>
      </template>
    </el-dialog>

    <!-- Mermaid 右键菜单 -->
    <div
      v-show="mermaidContextMenu.visible"
      class="mermaid-context-menu"
      :style="{
        left: mermaidContextMenu.x + 'px',
        top: mermaidContextMenu.y + 'px',
      }"
      @click.stop
    >
      <div class="menu-item" @click="copyMermaidImage">
        <el-icon><CopyDocument /></el-icon>
        <span>复制图片</span>
      </div>
      <div class="menu-item" @click="downloadMermaidPNG">
        <el-icon><Picture /></el-icon>
        <span>下载 PNG</span>
      </div>
    </div>
  </div>
</template>

<script>
import { defineAsyncComponent, h } from 'vue'
import { $on, $off, $once, $emit } from '../utils/gogocodeTransfer'
import {
  Upload as ElIconUpload,
  Share as ElIconShare,
  CopyDocument,
  Download,
  Picture,
} from '@element-plus/icons-vue'
import { useMainStore } from '@/stores/main'

import MarkdownIt from 'markdown-it'
import markdownItMultimdTable from 'markdown-it-multimd-table'
import markdownItTaskLists from 'markdown-it-task-lists'
// KaTeX 改为按需动态加载，只有文章包含数学公式时才加载
import { hasMathFormula, loadMarkdownItKatex } from '@/utils/katexLoader'
import axios from 'axios'
import {
  getLanguageMapping,
  preloadLanguageMapping,
} from '@/utils/languageUtils'
import {
  applyThemeFromArticle,
  resetTheme,
} from '@/composables/useArticleTheme'
import { decrypt } from '@/utils/crypto-utils'
import { syncTocPosition, getTocbot } from '@/utils/article-toc'
import {
  getArticleMeta,
  setDefaultMetaTags,
  updateMetaTags,
  fetchArticleMeta,
} from '@/utils/article-meta'
import {
  setupLanguageSwitchEventDelegation,
  handleMouseDown,
  handleTouchStart,
  handleLanguageSwitch,
  switchLanguage,
  fetchTranslation,
  updateUrlWithLanguage,
  initializeLanguageSettings,
  getDefaultTargetLanguage,
  getArticleAvailableLanguages,
  generateLanguageButtons,
} from '@/utils/article-language'
import {
  highlight,
  wrapTables,
  addLineNumbersWithCSS,
  addLoadingPlaceholders,
  detectAndLoadResources,
  renderMermaid,
  renderECharts,
  handleThemeChange,
  applyZoomButtonTheme,
  applyMermaidThemeStyles,
  toggleMermaidZoom,
  handleMermaidContextMenu,
  closeMermaidContextMenu,
  copyMermaidImage,
  downloadMermaidPNG,
  inlineSvgStyles,
  convertSvgToCanvas,
} from '@/utils/article-rendering'
import {
  openShareCardDialog,
  preloadHtml2Canvas,
  formatDate,
  generateQRCode,
  downloadShareCard,
  captureAndDownloadCard,
} from '@/utils/article-share-card'

const CommentLoading = {
  name: 'CommentLoading',
  render() {
    return h(
      'div',
      {
        style: {
          padding: '24px 0',
          textAlign: 'center',
          color: 'var(--text-color-secondary, #999)',
        },
      },
      '评论加载中...'
    )
  },
}

const CommentError = {
  name: 'CommentError',
  render() {
    return h(
      'div',
      {
        style: {
          padding: '24px 0',
          textAlign: 'center',
          color: 'var(--el-color-danger, #f56c6c)',
        },
      },
      '评论加载失败'
    )
  },
}

const AsyncComment = defineAsyncComponent({
  loader: () => import('./comment/comment'),
  delay: 200,
  timeout: 30000,
  suspensible: false,
  onError: (error, retry, fail, attempts) => {
    if (attempts <= 2) {
      retry()
      return
    }
    fail()
  },
  loadingComponent: CommentLoading,
  errorComponent: CommentError,
})

export default {
  components: {
    myFooter: defineAsyncComponent(() => import('./common/myFooter')),
    comment: AsyncComment,
    commentBox: defineAsyncComponent(() => import('./comment/commentBox')),
    proButton: defineAsyncComponent(() => import('./common/proButton')),
    process: defineAsyncComponent(() => import('./common/process')),
    videoPlayer: defineAsyncComponent(() => import('./common/videoPlayer')),
    ElIconUpload,
    ElIconShare,
    CopyDocument,
    Download,
    Picture,
  },
  data() {
    return {
      id: this.$route.params.id,
      lang: this.$route.params.lang,
      subscribe: false,
      article: {},
      decryptedVideoUrl: '',
      articleContentHtml: '',
      articleContentKey: Date.now(), // 强制重新渲染的key
      treeHoleList: [],
      weiYanDialogVisible: false,
      copyrightDialogVisible: false,
      newsTime: '',
      showPasswordDialog: false,
      password: '',
      tips: '',
      scrollTop: 0,
      hasInitTocbot: false,
      metaTags: null,
      metaTagRetryCount: 0,
      isLoadingMeta: false,
      currentLang: 'zh', // 默认中文
      isLoading: false,
      translatedTitle: '',
      translatedContent: '',
      tempComment: null, // 存储临时评论内容
      targetLanguage: 'en', // 目标语言
      targetLanguageName: 'English', // 目标语言名称
      sourceLanguage: 'zh', // 源语言
      sourceLanguageName: '中文', // 源语言名称
      languageMap: {}, // 语言映射
      availableLanguages: [], // 文章实际可用的翻译语言
      availableLanguageButtons: [], // 动态生成的语言按钮列表
      shareCardDialogVisible: false, // 卡片分享弹窗显示状态
      tocbotRefreshed: false, // 标记tocbot是否已在首次滚动时刷新
      tocbotRefreshTimer: null, // tocbot刷新定时器
      loadingArticleId: null, // 正在加载的文章ID（用于防止异步回调干扰）
      shouldLoadComments: false,
      commentObserver: null,
      mermaidContextMenu: {
        visible: false,
        x: 0,
        y: 0,
        currentContainer: null,
      },
      articleThemeConfig: null, // 文章主题配置（缓存，供 TOC 使用）
      paymentLoading: false, // 付费按钮加载状态
      verifyingPayment: false, // 验证支付状态
    }
  },
  head() {
    if (!this.metaTags) {
      return {
        title: 'POETIZE博客',
        meta: [],
      }
    }

    return {
      title: this.metaTags.title,
      meta: [
        { name: 'description', content: this.metaTags.description },
        { name: 'keywords', content: this.metaTags.keywords },
        { name: 'author', content: this.metaTags.author },
        { property: 'og:title', content: this.metaTags.title },
        { property: 'og:description', content: this.metaTags.description },
        { property: 'og:type', content: 'article' },
        { property: 'og:url', content: this.metaTags['og:url'] },
        { property: 'og:image', content: this.metaTags['og:image'] },
        { name: 'twitter:card', content: this.metaTags['twitter:card'] },
        { name: 'twitter:title', content: this.metaTags.title },
        { name: 'twitter:description', content: this.metaTags.description },
        { name: 'twitter:image', content: this.metaTags['twitter:image'] },
        {
          property: 'article:published_time',
          content: this.metaTags['article:published_time'],
        },
        {
          property: 'article:modified_time',
          content: this.metaTags['article:modified_time'],
        },
      ],
    }
  },
  async created() {
    // 重置组件状态，防止缓存问题
    this.resetComponentState()

    // 先初始化语言映射（从数据库统一配置读取）
    this.languageMap = await getLanguageMapping()

    // 然后初始化语言设置，确保语言状态正确
    await this.initializeLanguageSettings()

    if (!this.$common.isEmpty(this.id)) {
      // 首次加载时强制清空预渲染内容，确保Vue重新渲染
      this.articleContentHtml = ''
      this.articleContentKey = Date.now()

      this.getArticle(localStorage.getItem('article_password_' + this.id))

      if ('0' !== localStorage.getItem('showSubscribe')) {
        this.$notify.success('文章订阅', '点击文章下方订阅/取消订阅专栏', 15000)
        // 设置延时关闭提示
        setTimeout(() => {
          localStorage.setItem('showSubscribe', '0')
        }, 3000)
      }
    }

    // 检查是否有待执行的订阅操作
    this.checkPendingSubscribe()

    // 文章页面加载时触发看板娘检查
    this.$nextTick(() => {
      // 延迟触发事件，确保页面元素已加载
      setTimeout(() => {
        if (document && document.dispatchEvent) {
          document.dispatchEvent(new Event('checkWaifu'))
        }
      }, 1000)
    })
  },
  mounted() {
    window.addEventListener('scroll', this.onScrollPage)
    window.addEventListener('load', this.syncTocPosition)
    window.addEventListener('pageshow', this.syncTocPosition)
    window.addEventListener('resize', this.syncTocPosition)
    // 注意：不在这里调用getTocbot()，因为文章内容还没加载
    // getTocbot()会在getArticle()完成后的$nextTick中调用
    this.syncTocPosition()

    // 监听主题切换事件
    $on(this.$root, 'themeChanged', this.handleThemeChange)

    // 添加全局事件委托处理语言切换按钮点击
    this.setupLanguageSwitchEventDelegation()

    // 添加全局点击事件，关闭右键菜单
    document.addEventListener('click', this.closeMermaidContextMenu)

    // 注意：不需要实现JavaScript动态检测遮挡的响应式逻辑
    // 原因：通过CSS层叠上下文（.article-head z-index: 10 和 .article-container z-index: 1）
    // 已经彻底解决了语言切换按钮被遮挡的问题，无需动态调整按钮位置
    // 同时已注释掉 @media (max-width: 1050px) 中隐藏按钮的CSS规则

    // 添加看板娘初始化检查
    this.$nextTick(() => {
      // 检查当前配置是否启用看板娘
      const checkWaifuEnabled = () => {
        try {
          // 从本地存储获取配置
          const webInfoStr = localStorage.getItem('webInfo')
          if (webInfoStr) {
            const webInfoData = JSON.parse(webInfoStr)
            // 检查
            if (webInfoData.data) {
              return webInfoData.data.enableWaifu === true
            }
          }
          return this.mainStore.webInfo.enableWaifu === true
        } catch (e) {
          return false
        }
      }

      // 检查是否已加载Live2D
      const checkLive2DLoaded = () => {
        return (
          typeof window.loadlive2d === 'function' &&
          document.getElementById('waifu') &&
          document.getElementById('live2d')
        )
      }

      // 检查看板娘是否显示
      const checkWaifuVisible = () => {
        const waifu = document.getElementById('waifu')
        return (
          waifu &&
          waifu.style.display !== 'none' &&
          waifu.style.bottom !== '-500px'
        )
      }

      // 检查并在需要时通过事件触发看板娘检查
      setTimeout(() => {
        if (checkWaifuEnabled()) {
          if (!checkLive2DLoaded() || !checkWaifuVisible()) {
            // 使用事件驱动方式加载看板娘，避免直接操作DOM
            if (!localStorage.getItem('waifu-display')) {
              // 触发检查事件，让live2d.js完成初始化
              document.dispatchEvent(new Event('checkWaifu'))
            }
          }
        }
      }, 2000) // 延迟2秒检查，确保页面完全加载
    })

    // 检查是否有临时保存的评论
    this.checkTempComment()

    // 检查是否有保存的页面状态
    this.checkPageState()

    // 监听路由变化，检查是否从登录页面返回
    this.$watch(
      () => this.$route.query,
      (newQuery) => {
        if (newQuery.hasComment === 'true') {
          // 从登录页面返回且带有评论标记
          this.$nextTick(() => {
            this.checkTempComment()
          })
        }

        // 检查回复操作恢复标记
        if (newQuery.hasReplyAction === 'true') {
          // 从登录页面返回且带有回复操作标记
          this.$nextTick(() => {
            this.checkPageState()
          })
        }
      }
    )

    this.$nextTick(() => {
      this.setupCommentIntersectionObserver()
    })
  },
  unmounted() {
    window.removeEventListener('scroll', this.onScrollPage)
    window.removeEventListener('load', this.syncTocPosition)
    window.removeEventListener('pageshow', this.syncTocPosition)
    window.removeEventListener('resize', this.syncTocPosition)

    // 移除文章主题自定义 CSS 变量，恢复默认值
    resetTheme()

    // 移除主题切换事件监听
    $off(this.$root, 'themeChanged', this.handleThemeChange)

    // 清理语言切换事件监听器
    if (this.languageSwitchHandler) {
      document.removeEventListener('click', this.languageSwitchHandler, true)
      document.removeEventListener('touchend', this.languageSwitchHandler, true)
      document.removeEventListener(
        'mousedown',
        this.languageSwitchHandler,
        true
      )
      document.removeEventListener(
        'touchstart',
        this.languageSwitchHandler,
        true
      )
      this.languageSwitchHandler = null
    }

    // 清理FAB点击外部区域事件监听器
    if (this.fabClickOutsideHandler) {
      document.removeEventListener('click', this.fabClickOutsideHandler, true)
      this.fabClickOutsideHandler = null
    }

    document.removeEventListener('click', this.closeMermaidContextMenu)
    this.teardownCommentIntersectionObserver()
  },
  watch: {
    scrollTop(scrollTop, oldScrollTop) {
      // 滚动监听逻辑已移至home.vue的toolButton控制
    },
    '$route.params': function (newParams, oldParams) {
      // 检查文章ID或语言参数是否变化
      const newId = newParams.id
      const oldId = oldParams.id
      const newLang = newParams.lang
      const oldLang = oldParams.lang

      if (newId && newId !== this.id) {
        // 重置组件状态，防止显示旧数据
        this.resetComponentState()

        // 更新组件的id和lang数据
        this.id = newId
        this.lang = newLang

        // 重新初始化语言设置 - 关键修复：确保每次切换文章都重新初始化语言
        this.initializeLanguageSettings()
          .then(() => {
            // 语言初始化完成后再获取文章
            const password = localStorage.getItem('article_password_' + this.id)
            this.getArticle(password)
          })
          .catch((error) => {
            // 即使语言初始化失败，也要获取文章
            const password = localStorage.getItem('article_password_' + this.id)
            this.getArticle(password)
          })

        // 检查是否有待执行的订阅操作
        this.$nextTick(() => {
          this.checkPendingSubscribe()
        })
      } else if (newId === this.id && newLang !== oldLang) {
        // 同一文章，仅语言参数变化
        this.lang = newLang

        if (newLang && this.languageMap[newLang]) {
          if (this.currentLang !== newLang) {
            this.switchLanguage(newLang)
          }
        } else {
          // 如果语言参数无效，切换到默认源语言
          this.switchLanguage(this.sourceLanguage)
        }
      }
    },
  },
  computed: {
    mainStore() {
      return useMainStore()
    },
    articleTitle() {
      // 如果当前语言不是源语言且已有翻译标题，则显示翻译标题，否则显示原始标题
      return this.currentLang !== this.sourceLanguage && this.translatedTitle
        ? this.translatedTitle
        : this.article.articleTitle
    },

    // 全局评论开关 - 从系统配置中读取
    enableComment() {
      const sysConfig = this.mainStore.sysConfig
      // 默认为 true，如果配置不存在或配置值为 'true' 则显示评论
      if (!sysConfig || !sysConfig.enableComment) {
        return true
      }
      return (
        sysConfig.enableComment === 'true' || sysConfig.enableComment === true
      )
    },

    // 对话框居中由 centered-dialog.css 全局样式处理（移除了自定义top计算）
    shareCardDialogTop() {
      return '15vh' // 保留作为fallback，但实际由centered-dialog.css的flex居中处理
    },
  },
  beforeUnmount() {
    // 组件销毁时清理状态，防止影响下一个文章组件
    this.clearComponentState()
    this.teardownCommentIntersectionObserver()

    // 销毁tocbot实例
    if (window.tocbot) {
      try {
        window.tocbot.destroy()
      } catch (e) {
        // 忽略销毁失败
      }
    }

    // 清理定时器
    if (this.tocbotRefreshTimer) {
      clearTimeout(this.tocbotRefreshTimer)
      this.tocbotRefreshTimer = null
    }
  },
  methods: {
    /**
     * 创建 Markdown 渲染器，按需加载 KaTeX
     * @param {string} content - 要渲染的内容
     * @returns {Promise<Object>} MarkdownIt 实例
     */
    async createMarkdownRenderer(content) {
      const md = new MarkdownIt({ breaks: true })
        .use(markdownItMultimdTable)
        .use(markdownItTaskLists, {
          enabled: true,
          label: true,
          labelAfter: true
        })

      const defaultImageRenderer = md.renderer.rules.image
      md.renderer.rules.image = (tokens, idx, options, env, self) => {
        const token = tokens[idx]

        token.attrSet('loading', 'lazy')
        token.attrSet('decoding', 'async')

        if (defaultImageRenderer) {
          return defaultImageRenderer(tokens, idx, options, env, self)
        }

        return self.renderToken(tokens, idx, options)
      }
      
      // 只有检测到数学公式时才加载 katex
      if (hasMathFormula(content)) {
        const katexPlugin = await loadMarkdownItKatex()
        if (katexPlugin) {
          md.use(katexPlugin)
        }
      }
      
      return md
    },

    getDisplayedMarkdownContent() {
      if (
        this.currentLang &&
        this.currentLang !== this.sourceLanguage &&
        this.translatedContent
      ) {
        return this.translatedContent
      }

      return this.article?.articleContent || ''
    },

    async renderArticleBody(content, { setupCommentObserver = false } = {}) {
      const safeContent = content || ''
      const md = await this.createMarkdownRenderer(safeContent)
      // 使用 markdown-it 渲染标准 markdown
      let renderedHtml = md.render(safeContent)
      
      // 额外处理：很多时候文章里会有直接手写的 HTML <img /> 标签，或者 Vditor 插入的 HTML 图片
      // 这里通过正则确保所有图片都被强行加上 loading="lazy"
      renderedHtml = renderedHtml.replace(
        /<img(?![^>]*\bloading=['"]lazy['"])[^>]*>/gi,
        (match) => {
          return match.replace('<img', '<img loading="lazy" decoding="async"')
        }
      )
      
      this.articleContentHtml = renderedHtml
      this.articleContentKey = Date.now()

      await this.$nextTick()

      this.$common.imgShow('.entry-content img')
      this.normalizeTaskListCheckboxes()
      this.wrapTables()
      this.highlight()
      this.renderMermaid()
      this.renderECharts()
      this.addId()
      this.detectAndLoadResources()

      if (setupCommentObserver) {
        this.setupCommentIntersectionObserver()
      }
    },

    normalizeTaskListCheckboxes(container) {
      const root =
        container ||
        (this.$el ? this.$el.querySelector('.entry-content') : null) ||
        document.querySelector('.entry-content')
      if (!root) return

      const checkboxes = root.querySelectorAll(
        'li.task-list-item input[type="checkbox"], input.task-list-item-checkbox[type="checkbox"]'
      )

      checkboxes.forEach((checkbox) => {
        // 移除 disabled 属性以保证样式正常（非灰色）
        checkbox.removeAttribute('disabled')
        
        // 如果已经绑定过锁定逻辑，跳过
        if (checkbox.dataset.todoReadonlyBound === 'true') return

        const lockedChecked = checkbox.checked
        checkbox.dataset.todoReadonlyBound = 'true'
        
        // 核心锁定逻辑：阻止点击和状态改变
        const lock = (e) => {
          e.preventDefault()
          e.stopPropagation()
          checkbox.checked = lockedChecked
          return false
        }

        checkbox.addEventListener('click', lock, true)
        checkbox.addEventListener('change', lock, true)
        // 阻止键盘操作
        checkbox.addEventListener('keydown', (e) => {
           if (e.code === 'Space' || e.key === ' ') {
             lock(e)
           }
        }, true)
      })
    },

    // 处理分类标签拖拽开始事件
    handleSortDragStart(event) {
      // 构建分类页面的完整URL
      const baseUrl = window.location.origin
      const sortPath = `/sort/${this.article.sortId}?labelId=${this.article.labelId}`
      const sortUrl = `${baseUrl}${sortPath}`

      // 设置拖拽数据
      event.dataTransfer.effectAllowed = 'link'
      event.dataTransfer.setData('text/uri-list', sortUrl)
      event.dataTransfer.setData('text/plain', sortUrl)

      // 设置拖拽时显示的文本
      const title = `${this.article.sort.sortName} · ${this.article.label.labelName}`
      event.dataTransfer.setData(
        'text/html',
        `<a href="${sortUrl}">${title}</a>`
      )
    },

    // 重置组件状态，防止缓存问题
    resetComponentState() {
      this.article = {}
      this.decryptedVideoUrl = ''
      this.translatedTitle = ''
      this.translatedContent = ''
      this.articleContentHtml = ''
      this.articleContentKey = Date.now()
      this.isLoading = false
      this.shouldLoadComments = false
      this.teardownCommentIntersectionObserver()

      // 重置语言相关状态 - 这是关键修复
      this.currentLang = this.sourceLanguage || 'zh'
      this.availableLanguages = []
      this.availableLanguageButtons = []

      // 重置密码相关状态
      this.showPasswordDialog = false
      this.password = ''
      this.tips = ''

      // 重置订阅状态
      this.subscribe = false

      // 重置元标签相关状态
      this.metaTags = null
      this.metaTagRetryCount = 0
      this.isLoadingMeta = false

      // 重置目录相关状态
      this.tocbotRefreshed = false
      if (this.tocbotRefreshTimer) {
        clearTimeout(this.tocbotRefreshTimer)
        this.tocbotRefreshTimer = null
      }

      // 重置正在加载的文章ID（防止旧文章的异步回调影响新文章）
      this.loadingArticleId = null

      // 销毁旧的tocbot实例（路由切换时）
      if (window.tocbot) {
        try {
          window.tocbot.destroy()
        } catch (e) {
          // 忽略销毁失败的错误
        }
      }
    },

    // 清理组件状态
    clearComponentState() {
      // 清理其他可能的异步操作
      this.loading = false

      // 清理翻译内容
      this.translatedTitle = ''
      this.translatedContent = ''
    },

    async verifyPayment() {
      if (this.$common.isEmpty(this.mainStore.currentUser)) {
        this.$message.warning('请先登录')
        return
      }
      this.verifyingPayment = true
      try {
        const res = await this.$http.get(
          this.$constant.baseURL + '/payment/checkPayment',
          { articleId: this.article.id }
        )
        if (res.code === 200 && res.data === true) {
          this.$message.success('验证成功！正在刷新文章...')
          this.getArticle()
        } else {
          this.$message.info('暂未查到支付记录，如已支付请稍后再试')
        }
      } catch (e) {
        this.$message.error('验证失败，请稍后重试')
      } finally {
        this.verifyingPayment = false
      }
    },

    async handlePayment() {
      if (this.$common.isEmpty(this.mainStore.currentUser)) {
        this.$message.warning('请先登录后再进行付费操作')
        return
      }
      this.paymentLoading = true
      try {
        // 会员专属文章传 articleId=0，其他传实际文章ID
        const payArticleId = this.article.payType === 2 ? 0 : this.article.id
        const res = await this.$http.get(
          this.$constant.baseURL + '/payment/getPaymentUrl',
          { articleId: payArticleId }
        )
        if (res.code === 200 && res.data) {
          window.open(res.data, '_blank')
          // 轮询检查支付状态
          this.$message({
            message: '请在新窗口完成支付，支付后将自动刷新',
            type: 'info',
            duration: 10000
          })
          let checkCount = 0
          const checkInterval = setInterval(async () => {
            checkCount++
            if (checkCount > 60) { // 5分钟超时
              clearInterval(checkInterval)
              return
            }
            try {
              const checkRes = await this.$http.get(
                this.$constant.baseURL + '/payment/checkPayment',
                { articleId: this.article.id }
              )
              if (checkRes.code === 200 && checkRes.data === true) {
                clearInterval(checkInterval)
                this.$message.success('支付成功！正在刷新文章...')
                this.getArticle()
              }
            } catch (e) {
              // 静默忽略检查错误
            }
          }, 5000)
        } else {
          this.$message.error(res.message || '获取支付链接失败，请稍后重试')
        }
      } catch (error) {
        this.$message.error('网络错误，请稍后重试')
      } finally {
        this.paymentLoading = false
      }
    },

    subscribeLabel() {
      // 首先显示确认订阅对话框
      const confirmMessage = this.subscribe
        ? '确认取消订阅专栏【' + this.article.label.labelName + '】？'
        : '确认订阅专栏【' +
          this.article.label.labelName +
          '】？订阅专栏后，该专栏发布新文章将通过邮件通知订阅用户。'

      const confirmTitle = this.subscribe ? '取消订阅' : '文章订阅'

      this.$confirm(confirmMessage, confirmTitle, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
      })
        .then(() => {
          // 用户确认订阅意图后，检查登录状态
          if (this.$common.isEmpty(this.mainStore.currentUser)) {

            // 立即保存订阅意图并跳转到登录页面
            this.saveSubscribeIntentAndRedirectToLogin()
            return
          }

          // 已登录，直接执行订阅操作
          this.executeSubscribe()
        })
        .catch(() => {
          this.$message({
            type: 'success',
            message: '已取消!',
          })
        })
    },

    // 保存订阅意图并跳转到登录页面
    saveSubscribeIntentAndRedirectToLogin() {
      const subscribeIntent = {
        articleId: this.id,
        labelId: this.article.labelId,
        labelName: this.article.label.labelName,
        action: this.subscribe ? 'unsubscribe' : 'subscribe',
        timestamp: Date.now(),
      }

      // 保存订阅意图到localStorage
      localStorage.setItem('pendingSubscribe', JSON.stringify(subscribeIntent))

      // 使用统一的登录跳转函数
      this.$common.redirectToLogin(
        this.$router,
        {
          message: '请先登录！',
        },
        this
      )
    },

    // 执行订阅操作
    executeSubscribe() {
      this.$http
        .get(this.$constant.baseURL + '/user/subscribe', {
          labelId: this.article.labelId,
          flag: !this.subscribe,
        })
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            this.mainStore.loadCurrentUser(res.data)
          }
          this.subscribe = !this.subscribe

          // 显示成功消息
          const message = this.subscribe ? '订阅成功！' : '取消订阅成功！'
          this.$message({
            message: message,
            type: 'success',
          })
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error',
          })
        })
    },

    // 检查并处理待执行的订阅操作
    checkPendingSubscribe() {
      const pendingSubscribe = localStorage.getItem('pendingSubscribe')
      if (!pendingSubscribe) {
        return
      }

      try {
        const subscribeIntent = JSON.parse(pendingSubscribe)

        // 检查是否是当前文章的订阅意图
        if (subscribeIntent.articleId === this.id) {
          // 清除待执行的订阅意图
          localStorage.removeItem('pendingSubscribe')

          // 检查用户是否已登录
          if (!this.$common.isEmpty(this.mainStore.currentUser)) {
            // 延迟执行订阅操作，确保页面数据已加载完成
            this.$nextTick(() => {
              setTimeout(() => {
                this.executeSubscribe()
              }, 500)
            })
          }
        }
      } catch (error) {
        localStorage.removeItem('pendingSubscribe')
      }
    },
    submitPassword() {
      if (this.$common.isEmpty(this.password)) {
        this.$message({
          message: '请先输入密码！',
          type: 'error',
        })
        return
      }

      this.getArticle(this.password)
    },
    deleteTreeHole(id) {
      if (this.$common.isEmpty(this.mainStore.currentUser)) {
        // 使用统一的登录跳转函数
        this.$common.redirectToLogin(
          this.$router,
          {
            message: '请先登录！',
          },
          this
        )
        return
      }

      this.$confirm('确认删除？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success',
        center: true,
      })
        .then(() => {
          this.$http
            .get(this.$constant.baseURL + '/weiYan/deleteWeiYan', { id: id })
            .then((res) => {
              this.$message({
                type: 'success',
                message: '删除成功!',
              })
              this.getNews()
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: 'error',
              })
            })
        })
        .catch(() => {
          this.$message({
            type: 'success',
            message: '已取消删除!',
          })
        })
    },
    submitWeiYan(content) {
      let weiYan = {
        content: content,
        createTime: this.newsTime,
        source: this.article.id,
      }

      this.$http
        .post(this.$constant.baseURL + '/weiYan/saveNews', weiYan)
        .then((res) => {
          this.weiYanDialogVisible = false
          this.newsTime = ''
          this.getNews()
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error',
          })
        })
    },
    getNews() {
      this.$http
        .post(this.$constant.baseURL + '/weiYan/listNews', {
          current: 1,
          size: 9999,
          source: this.article.id,
        })
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            res.data.records.forEach((c) => {
              c.content = c.content.replace(
                /\n{2,}/g,
                '<div style="height: 12px"></div>'
              )
              c.content = c.content.replace(/\n/g, '<br/>')
              c.content = this.$common.faceReg(c.content)
              c.content = this.$common.pictureReg(c.content)
            })
            this.treeHoleList = res.data.records
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error',
          })
        })
    },
    onScrollPage() {
      const scrollingElement =
        document.scrollingElement || document.documentElement || document.body
      this.scrollTop = scrollingElement ? scrollingElement.scrollTop : 0
      this.syncTocPosition()

      // 在用户首次滚动时刷新tocbot，确保位置计算准确
      if (!this.tocbotRefreshed && window.tocbot && window.tocbot.refresh) {
        if (this.tocbotRefreshTimer) {
          clearTimeout(this.tocbotRefreshTimer)
        }
        this.tocbotRefreshTimer = setTimeout(() => {
          if (window.tocbot && window.tocbot.refresh) {
            window.tocbot.refresh()
            this.tocbotRefreshed = true
          }
        }, 50)
      }
    },
    syncTocPosition,
    getTocbot,
    addId() {
      const entryContent = document.querySelector('.entry-content')
      if (entryContent) {
        const headings = entryContent.querySelectorAll('h1, h2, h3, h4, h5, h6')
        headings.forEach((heading, index) => {
          if (!heading.id) {
            heading.id = 'toc-' + index
          }
        })
      }
    },
    getArticleMeta,
    setDefaultMetaTags,
    updateMetaTags,
    getArticle(password) {
      this.isLoading = true

      // 设置正在加载的文章ID（在this.id更新之后调用，所以this.id已经是新文章ID）
      this.loadingArticleId = this.id

      // 重置状态，防止显示旧数据
      this.article = {}
      this.articleContentHtml = ''
      this.translatedTitle = ''
      this.translatedContent = ''
      this.tocbotRefreshed = false // 重置tocbot刷新标志
      this.shouldLoadComments = false
      this.teardownCommentIntersectionObserver()

      // 使用Promise.all并行处理所有请求
      // 如果当前语言不是源语言，在第一次请求时就带上语言参数
      const articleParams = { id: this.id, password: password }
      if (this.currentLang && this.currentLang !== this.sourceLanguage) {
        articleParams.language = this.currentLang
      }

      Promise.all([
        this.$http.get(
          this.$constant.baseURL + '/article/getArticleById',
          articleParams
        ),
        this.$http.post(this.$constant.baseURL + '/weiYan/listNews', {
          current: 1,
          size: 9999,
          source: this.id,
        }),
        this.fetchArticleMeta(),
      ])
        .then(async ([articleRes, newsRes]) => {
          // 处理文章数据
          if (!this.$common.isEmpty(articleRes.data)) {
            this.article = articleRes.data

            // 解密视频URL
            if (this.article.videoUrl) {
              decrypt(this.article.videoUrl).then(url => {
                this.decryptedVideoUrl = url || ''
              })
            }

            // 在渲染内容之前，先同步应用文章主题（从接口合并返回，无额外请求）
            // 这样标题装饰在首次渲染时就是正确的，彻底避免闪烁
            if (this.article.articleThemeConfig) {
              this.articleThemeConfig = applyThemeFromArticle(this.article.articleThemeConfig)
            }

            // 检查当前语言状态，决定显示内容
            // 确定要渲染的内容
            const contentToRender =
              this.currentLang !== this.sourceLanguage &&
              this.article.translatedContent
                ? this.article.translatedContent
                : this.article.articleContent

            // 判断显示原文还是翻译
            if (
              this.currentLang !== this.sourceLanguage &&
              this.article.translatedContent
            ) {
              // 显示翻译内容（后端已一次性返回）
              this.translatedTitle = this.article.translatedTitle
              this.translatedContent = this.article.translatedContent
            } else {
              // 显示原文
              this.translatedTitle = ''
              this.translatedContent = ''
            }
            await this.renderArticleBody(contentToRender, {
              setupCommentObserver: true,
            })

            // 确保样式正确应用的保险措施
            setTimeout(() => {
              // 检查是否有代码块没有正确处理
              const entryContent = document.querySelector('.entry-content')
              if (entryContent) {
                const unprocessedBlocks = entryContent.querySelectorAll(
                  'pre:not(.highlight-wrap)'
                )
                if (unprocessedBlocks.length > 0) {
                  this.highlight()
                  this.renderMermaid()
                  this.renderECharts()
                }
              }
            }, 1000)

            if (!this.$common.isEmpty(password)) {
              localStorage.setItem('article_password_' + this.id, password)
            }
            this.showPasswordDialog = false
            if (
              !this.$common.isEmpty(this.mainStore.currentUser) &&
              !this.$common.isEmpty(this.mainStore.currentUser.subscribe)
            ) {
              this.subscribe = JSON.parse(
                this.mainStore.currentUser.subscribe
              ).includes(this.article.labelId)
            }

            // 获取文章可用的翻译语言并生成动态按钮
            this.getArticleAvailableLanguages()
          } else {
            // 文章数据为空，说明文章不存在，跳转到404页面
            this.$router.push('/404')
            return
          }

          // 处理"最新进展"数据
          if (!this.$common.isEmpty(newsRes.data)) {
            newsRes.data.records.forEach((c) => {
              c.content = c.content.replace(
                /\n{2,}/g,
                '<div style="height: 12px"></div>'
              )
              c.content = c.content.replace(/\n/g, '<br/>')
              c.content = this.$common.faceReg(c.content)
              c.content = this.$common.pictureReg(c.content)
            })
            this.treeHoleList = newsRes.data.records
          }
        })
        .catch((error) => {
          console.error('获取文章失败:', error)

          // 统一错误处理
          if (
            error &&
            error.message &&
            '密码错误' === error.message.substr(0, 4)
          ) {
            // 密码错误，显示密码输入框
            if (!this.$common.isEmpty(password)) {
              localStorage.removeItem('article_password_' + this.id)
              this.$message({
                message: '密码错误，请重新输入！',
                type: 'error',
                customClass: 'message-index',
              })
            }
            this.tips = error.message.substr(4)
            this.showPasswordDialog = true
          } else if (
            error &&
            error.message &&
            (error.message.includes('文章不存在') ||
              error.message.includes('文章未找到') ||
              error.message.includes('404') ||
              error.message.includes('Not Found'))
          ) {
            // 文章不存在，跳转到404页面
            this.$router.push('/404')
            return
          } else {
            // 其他错误（网络错误等），显示错误消息但不跳转
            this.$message({
              message: error ? error.message : '加载失败，请重试',
              type: 'error',
              customClass: 'message-index',
            })
          }
        })
        .finally(() => {
          this.isLoading = false
          this.$nextTick(() => {
            this.normalizeTaskListCheckboxes()
          })
        })
    },
    fetchArticleMeta,
    highlight,
    
    /**
     * 处理表格样式 - 将表格包装到 table-wrapper 中以应用样式
     * 此方法独立于代码高亮，确保无论是否有代码块，表格都能正确显示样式
     */
    wrapTables,

    /**
     * 使用CSS计数器添加行号
     */
    addLineNumbersWithCSS,

    // 给代码块添加 loading 占位符
    addLoadingPlaceholders,

    // 检测文章内容中需要加载的资源（异步并行，不阻塞渲染）
    // 注意：此方法应在 $nextTick 中调用，确保 DOM 已渲染
    detectAndLoadResources,

    renderMermaid,

    // 渲染 ECharts 图表
    renderECharts,

    // 处理主题切换事件
    handleThemeChange,

    // 应用放大按钮主题样式
    applyZoomButtonTheme,

    // 应用 Mermaid 主题样式（容器背景 + 线条/箭头颜色）
    // 注意：使用 default 主题时，节点颜色和文字颜色保持不变，只需调整线条颜色
    applyMermaidThemeStyles,

    // 切换Mermaid图表的放大/缩小状态
    toggleMermaidZoom,

    // 处理 Mermaid 右键菜单
    handleMermaidContextMenu,

    // 关闭 Mermaid 右键菜单
    closeMermaidContextMenu,

    // 复制 Mermaid 图片
    copyMermaidImage,

    // 下载 Mermaid PNG
    downloadMermaidPNG,

    // 辅助方法：内联 SVG 样式
    inlineSvgStyles,

    // 辅助方法：SVG 转 Canvas
    convertSvgToCanvas,

    setupLanguageSwitchEventDelegation,
    handleMouseDown,
    handleTouchStart,
    handleLanguageSwitch,
    switchLanguage,
    fetchTranslation,
    updateUrlWithLanguage,
    /**
     * 检查是否有临时保存的评论
     */
    checkTempComment() {
      const articleId = this.id
      const tempCommentKey = `tempComment_${articleId}`

      try {
        const savedComment = localStorage.getItem(tempCommentKey)
        if (savedComment) {
          const commentData = JSON.parse(savedComment)

          // 检查是否过期(24小时)
          const now = Date.now()
          const commentAge = now - commentData.timestamp

          if (commentAge < 24 * 60 * 60 * 1000) {
            this.tempComment = commentData.content
            this.shouldLoadComments = true

            // 延迟一点时间确保评论组件已加载
            setTimeout(() => {
              // 使用事件总线将评论内容发送到评论框组件
              $emit(this.$bus, 'restore-comment', this.tempComment)

              // 提示用户
              this.$message({
                message: '已恢复您之前的评论内容',
                type: 'success',
              })

              // 滚动到评论区
              this.$nextTick(() => {
                const commentElement = document.querySelector('.comment-head')
                if (commentElement) {
                  commentElement.scrollIntoView({ behavior: 'smooth' })
                }
              })

              // 清除临时评论
              localStorage.removeItem(tempCommentKey)
            }, 500)
          } else {
            // 过期则删除
            localStorage.removeItem(tempCommentKey)
          }
        }
      } catch (error) {
        console.error('恢复评论出错:', error)
        localStorage.removeItem(tempCommentKey)
      }
    },

    /**
     * 检查是否有保存的页面状态
     */
    checkPageState() {
      const articleId = this.id
      const pageStateKey = `pageState_${articleId}`

      try {
        const savedState = localStorage.getItem(pageStateKey)
        if (savedState) {
          const stateData = JSON.parse(savedState)

          // 检查是否过期(1小时)
          const now = Date.now()
          const stateAge = now - stateData.timestamp

          if (stateAge < 60 * 60 * 1000) {
            this.shouldLoadComments = true

            // 延迟一点时间确保评论组件已加载
            setTimeout(() => {
              // 使用事件总线将状态数据发送到评论组件
              $emit(this.$bus, 'restore-page-state', stateData)

              // 恢复滚动位置
              if (stateData.scrollPosition) {
                window.scrollTo({
                  top: stateData.scrollPosition,
                  behavior: 'smooth',
                })
              }

              // 提示用户
              this.$message({
                message: '已恢复您的操作状态',
                type: 'success',
              })

              // 清除保存的状态
              localStorage.removeItem(pageStateKey)
            }, 1000) // 延迟确保评论组件完全加载
          } else {
            // 过期则删除
            localStorage.removeItem(pageStateKey)
          }
        }
      } catch (error) {
        console.error('恢复页面状态出错:', error)
        localStorage.removeItem(pageStateKey)
      }
    },

    setupCommentIntersectionObserver() {
      if (this.shouldLoadComments) {
        this.teardownCommentIntersectionObserver()
        return
      }

      this.teardownCommentIntersectionObserver()

      const target = this.$refs.commentSentinel
      if (!target) {
        return
      }

      if (
        typeof window === 'undefined' ||
        typeof IntersectionObserver === 'undefined'
      ) {
        this.shouldLoadComments = true
        return
      }

      this.commentObserver = new IntersectionObserver(
        (entries) => {
          const shouldLoad = entries.some(
            (entry) => entry.isIntersecting || entry.intersectionRatio > 0
          )
          if (shouldLoad) {
            this.shouldLoadComments = true
            this.teardownCommentIntersectionObserver()
          }
        },
        {
          root: null,
          rootMargin: '800px 0px',
          threshold: 0,
        }
      )

      this.commentObserver.observe(target)
    },

    teardownCommentIntersectionObserver() {
      if (this.commentObserver) {
        try {
          this.commentObserver.disconnect()
        } catch (e) {
        } finally {
          this.commentObserver = null
        }
      }
    },

    /**
     * 初始化语言设置
     * 修复重复调用 /api/translation/default-lang 接口的问题
     * 统一处理语言配置获取和语言设置逻辑
     */
    initializeLanguageSettings,
    getDefaultTargetLanguage,
    getArticleAvailableLanguages,
    generateLanguageButtons,

    openShareCardDialog,
    preloadHtml2Canvas,
    formatDate,
    generateQRCode,
    downloadShareCard,
    captureAndDownloadCard,
  },
  emits: ['restore-comment', 'restore-page-state'],
}
</script>

<style scoped>
.article-head {
  height: 40vh;
  position: relative;
  z-index: 10;
}
.article-head > * {
  pointer-events: auto;
}
.article-head .el-image {
  pointer-events: none;
}
.article-head .el-image * {
  pointer-events: none;
}
.article-image::before {
  position: absolute;
  width: 100%;
  height: 100%;
  background-color: var(--miniMask);
  content: '';
  z-index: 1;
  pointer-events: none;
}
.error-text {
  font-size: 22px;
  line-height: 1.8;
  letter-spacing: 2px;
  color: var(--white);
  padding: 20px;
  text-align: center;
  word-break: break-word;
}
.article-info-container {
  position: absolute;
  bottom: 15px;
  left: 20%;
  color: var(--white);
  z-index: 1000;
}
.article-info-news {
  position: absolute;
  bottom: 10px;
  right: 20%;
  cursor: pointer;
  animation: scale 1s ease-in-out infinite;
}
.article-title {
  font-size: 28px;
  margin-bottom: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}
.article-info {
  font-size: 14px;
  user-select: none;
}
.article-info i {
  margin-right: 6px;
}
.article-info span:not(:last-child) {
  margin-right: 5px;
}
.article-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  position: relative;
  z-index: 1;
}
.paywall-block {
  position: relative;
  margin-top: -80px;
  padding-top: 0;
  z-index: 2;
}

.paywall-fade {
  height: 120px;
  background: linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.85) 60%,
    rgba(255, 255, 255, 1) 100%
  );
  pointer-events: none;
}

body.dark-mode .paywall-fade {
  background: linear-gradient(
    to bottom,
    transparent 0%,
    var(--background) 100%
  );
}

.paywall-card {
  position: relative;
  text-align: center;
  padding: 40px 30px;
  border-radius: 8px;
  background: white;
  border: 2px dashed #ff4d79;
  margin-top: 30px;
  box-shadow: 0 4px 16px rgba(255, 77, 121, 0.08);
}

.paywall-badge {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: #ff4d79;
  color: white;
  width: 200px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border-radius: 30px;
  font-size: 18px;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(255, 77, 121, 0.3);
  letter-spacing: 2px;
}

.paywall-hidden-prompt {
  color: #ff4d79;
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.paywall-desc {
  font-size: 15px;
  color: var(--greyFont, #666);
  margin-bottom: 24px;
  line-height: 1.6;
}

.paywall-desc strong {
  color: #ff4d79;
  font-size: 22px;
}

.paywall-actions {
  margin-bottom: 16px;
}

.paywall-actions .el-button {
  padding: 14px 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  background-color: #ff4d79;
  border-color: #ff4d79;
}

.paywall-actions .el-button:hover,
.paywall-actions .el-button:focus {
  background-color: #ff6b8e;
  border-color: #ff6b8e;
  color: white;
}

.paywall-meta {
  font-size: 13px;
  color: var(--greyFont, #999);
}

.paywall-verify {
  margin-top: 10px;
  font-size: 12px;
}

.paywall-verify a {
  color: var(--greyFont, #aaa);
  text-decoration: none;
  transition: color 0.2s;
}

.paywall-verify a:hover {
  color: #409EFF;
}

.paywall-verify a.checking {
  color: #409EFF;
  pointer-events: none;
}

.article-update-time {
  color: var(--greyFont);
  font-size: 12px;
  margin: 20px 0;
  user-select: none;
}
blockquote {
  line-height: 2;
  border-left: 0.2rem solid var(--blue);
  padding: 10px 1rem;
  background-color: var(--azure);
  border-radius: 4px;
  margin: 0 0 40px 0;
  user-select: none;
  color: #333; /* 固定黑色，不受 im.css 暗色模式影响 */
}
.article-sort {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}
.article-sort span {
  padding: 0.25em 0.7em;
  background-color: var(--themeBackground);
  border-radius: 5px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--white);
  transition: background-color 0.3s ease, transform 0.3s ease, opacity 0.3s ease;
  margin-right: 25px;
  cursor: pointer;
  user-select: none;
  transform: translateZ(0);
}
.article-sort span:hover {
  background-color: var(--red);
}
.article-like {
  color: var(--red) !important;
}
.article-like-icon {
  font-size: 60px;
  cursor: pointer;
  color: var(--greyFont);
  transition: color 0.5s ease, transform 0.5s ease;
  border-radius: 50%;
  margin-bottom: 20px;
  transform: translateZ(0);
}
.article-like-icon:hover {
  transform: rotate(360deg);
}
.subscribe-button {
  background: rgb(119, 48, 152);
  width: 110px;
  padding: 0.5em 0;
  font-size: 16px;
  line-height: 1.5;
  text-align: center;
  color: var(--white);
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.3s ease,
    box-shadow 0.3s ease;
  user-select: none;
  transform: translateZ(0);
}
.subscribe-button i {
  margin-left: 0;
}
.subscribe-button:hover {
  background: rgb(99, 28, 132);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(119, 48, 152, 0.3);
}
.subscribe-button.subscribed {
  background: rgb(76, 175, 80);
}
.subscribe-button.subscribed:hover {
  background: rgb(56, 155, 60);
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.3);
}
.share-card-button {
  background: #ff416c;
  width: 110px;
  padding: 0.5em 0;
  font-size: 16px;
  line-height: 1.5;
  text-align: center;
  color: var(--white);
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.3s ease,
    box-shadow 0.3s ease;
  user-select: none;
  margin-left: 15px;
  transform: translateZ(0);
}
.share-card-button i {
  margin-left: 0;
}
.share-card-button:hover {
  background: #e63a5f;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(255, 65, 108, 0.3);
}
.share-card-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px;
  background: transparent;
}
.share-card-preview {
  background: hsla(0, 0%, 100%, 0.7019607843137254);
  border-radius: 12px;
  padding: 25px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  position: relative;
}
.card-avatar-container {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 12px;
}
.card-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
.card-date {
  font-size: 13px;
  color: #00000091;
  margin-bottom: 12px;
  font-weight: 400;
}
.card-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 18px;
  line-height: 1.4;
  word-wrap: break-word;
}
.card-cover {
  width: 100%;
  height: 220px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 18px;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.card-footer {
  margin-top: 15px;
}
.card-author {
  text-align: right;
  font-size: 14px;
  color: #00000091;
  margin-bottom: 15px;
  font-weight: 500;
}
.card-divider {
  width: 100%;
  margin-top: 0;
  margin-bottom: 10px;
  border: 1px solid hsla(0, 0%, 60%, 0.10196078431372549);
}
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-brand {
  font-size: 20px;
  color: #00000091;
  font-family: 'Arial', sans-serif;
  line-height: 1;
  margin: auto 0;
}
.card-qrcode {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card-qrcode img {
  width: 100%;
  height: 100%;
}
@media (max-width: 768px) {
  .share-card-preview {
    max-width: 100%;
    padding: 18px;
  }
  .card-avatar {
    width: 35px;
    height: 35px;
  }
  .card-date {
    font-size: 12px;
  }
  .card-title {
    font-size: 18px;
    margin-bottom: 15px;
  }
  .card-cover {
    height: 180px;
  }
  .card-author {
    font-size: 13px;
  }
  .card-brand {
    font-size: 18px;
    letter-spacing: 1px;
  }
  .card-qrcode {
    width: 50px;
    height: 50px;
  }
  .share-card-button {
    margin-left: 10px;
    width: 100px;
    font-size: 14px;
  }
}
.process-wrap {
  margin: 0 0 40px;
}
.process-wrap hr {
  position: relative;
  margin: 10px auto 60px;
  border: 2px dashed var(--lightGreen);
  overflow: visible;
}
.process-wrap hr:before {
  position: absolute;
  top: -14px;
  left: 5%;
  color: var(--lightGreen);
  content: '❄';
  font-size: 30px;
  line-height: 1;
  transition: transform 1s ease-in-out, left 1s ease-in-out;
  will-change: transform, left;
  transform: translateZ(0);
}
.process-wrap hr:hover:before {
  left: calc(95% - 20px);
}
.process-wrap :deep(.el-collapse-item__header){
  border-bottom: unset;
  font-size: 20px;
  background-color: var(--background);
  color: var(--lightGreen);
}
.process-wrap :deep(.el-collapse-item__wrap){
  background-color: var(--background);
}
.process-wrap .el-collapse {
  border-top: unset;
  border-bottom: unset;
}
.process-wrap :deep(.el-collapse-item__wrap){
  border-bottom: unset;
}
.password-content {
  font-size: 13px;
  color: var(--maxGreyFont);
  line-height: 1.5;
}
.copyright-container {
  color: var(--black);
  line-height: 2.5;
  padding: 0 30px 10px;
  font-size: 16px;
}
@media screen and (max-width: 700px) {
  .article-info-container {
    left: 20px;
    max-width: 320px;
    z-index: 1000;
  }
  .article-info-news {
    right: 20px;
  }
}
.language-switch-container {
  position: relative;
  z-index: 1;
  width: 100%;
  pointer-events: none;
  margin-bottom: 15px;
  clear: both;
  isolation: isolate;
  transform: translateZ(0);
}
.language-switch-container * {
  pointer-events: auto;
}
.article-language-switch {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  margin-bottom: 20px;
  margin-left: 10px;
  pointer-events: auto;
  transform: translateZ(0);
  will-change: transform;
}
.article-language-switch .el-button-group {
  position: relative;
  z-index: 1;
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.2);
  border-radius: 6px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  pointer-events: auto;
  transform: translateZ(0);
  isolation: isolate;
}
.article-language-switch .el-button {
  position: relative;
  z-index: 1;
  padding: 8px 15px;
  font-weight: 500;
  font-size: 13px;
  transition: background-color 0.3s ease, border-color 0.3s ease,
    color 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  user-select: none;
  pointer-events: auto !important;
  touch-action: manipulation;
  transform: translateZ(0);
  isolation: isolate;
}
.article-language-switch .el-button,
.article-language-switch .el-button *,
.article-language-switch .el-button::before,
.article-language-switch .el-button::after {
  pointer-events: auto !important;
}
.article-language-switch .el-button:hover {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(255, 255, 255, 1);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
.article-language-switch .el-button--primary {
  background-color: var(--themeBackground) !important;
  border-color: var(--themeBackground) !important;
  color: var(--white) !important;
}
.article-language-switch .el-button--primary:hover {
  background-color: var(--themeBackground) !important;
  border-color: var(--themeBackground) !important;
  opacity: 0.9;
}
.article-language-switch .el-button.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
@media (max-width: 768px) {
  .language-switch-container {
    position: relative;
    z-index: 1;
    margin-bottom: 10px;
    pointer-events: none;
    isolation: isolate;
    transform: translateZ(0);
  }
  .article-language-switch {
    position: relative;
    z-index: 1;
    margin-top: 15px;
    margin-bottom: 10px;
    margin-left: 15px;
    pointer-events: auto;
    transform: translateZ(0);
    will-change: transform;
  }
  .article-language-switch .el-button {
    position: relative;
    z-index: 1;
    padding: 6px 12px;
    font-size: 12px;
    min-height: 32px;
    pointer-events: auto;
    touch-action: manipulation;
    transform: translateZ(0);
    isolation: isolate;
  }
  .article-language-switch .el-button-group {
    position: relative;
    z-index: 1;
    pointer-events: auto;
    transform: translateZ(0);
    isolation: isolate;
  }
}
:deep(.mermaid-container){
  position: relative;
  margin: 20px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  overflow-x: auto;
  text-align: center;
  transition: all 0.3s ease;
}
body.dark-mode :deep(.mermaid-container){
  background: #2d2d2d !important;
}
:deep(.mermaid-container svg){
  max-width: 100%;
  height: auto;
}

/* ========== 暗色模式 Mermaid 图表 SVG 线条/箭头样式 ========== */
/* 注意：使用 default 主题时，节点颜色和文字颜色保持不变，只需调整线条、箭头和特定文字颜色 */

/* 暗色模式下的连接线颜色 */
body.dark-mode :deep(.mermaid-container svg path.relation),
body.dark-mode :deep(.mermaid-container svg line) {
  stroke: #a0a0a0 !important;
}

/* 暗色模式下的 marker 箭头颜色 */
body.dark-mode :deep(.mermaid-container svg marker path) {
  fill: #a0a0a0 !important;
  stroke: #a0a0a0 !important;
}

/* 边标签（箭头上的文字）- 显示在深色容器背景上 */
body.dark-mode :deep(.mermaid-container svg .edgeLabel),
body.dark-mode :deep(.mermaid-container svg .edgeLabel text),
body.dark-mode :deep(.mermaid-container svg .edgeLabel span),
body.dark-mode :deep(.mermaid-container svg .labelText) {
  fill: #e0e0e0 !important;
  color: #e0e0e0 !important;
}

/* 图表标题 */
body.dark-mode :deep(.mermaid-container svg .titleText),
body.dark-mode :deep(.mermaid-container svg .title),
body.dark-mode :deep(.mermaid-container svg .pieTitleText) {
  fill: #e0e0e0 !important;
}

/* 序列图消息文字 */
body.dark-mode :deep(.mermaid-container svg .messageText),
body.dark-mode :deep(.mermaid-container svg .sequenceNumber),
body.dark-mode :deep(.mermaid-container svg .loopText) {
  fill: #e0e0e0 !important;
}

/* 甘特图分区标题和外部任务标签 */
body.dark-mode :deep(.mermaid-container svg .sectionTitle),
body.dark-mode :deep(.mermaid-container svg .taskTextOutsideRight),
body.dark-mode :deep(.mermaid-container svg .taskTextOutsideLeft) {
  fill: #e0e0e0 !important;
}
:deep(.mermaid-zoom-btn){
  position: absolute;
  top: 10px;
  right: 10px;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  z-index: 10;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(4px);
}
:deep(.mermaid-zoom-btn:hover){
  background: rgba(255, 255, 255, 1);
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transform: scale(1.05);
}
:deep(.mermaid-zoom-btn:active){
  transform: scale(0.95);
}
:deep(.mermaid-zoom-btn .zoom-icon){
  width: 20px;
  height: 20px;
  color: #333;
  transition: color 0.2s ease;
}
:deep(.mermaid-zoom-btn:hover .zoom-icon){
  color: #409eff;
}
body.dark-mode :deep(.mermaid-zoom-btn),
.dark-mode :deep(.mermaid-zoom-btn){
  background: rgba(55, 55, 55, 0.95);
  border: 1px solid #555;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
body.dark-mode :deep(.mermaid-zoom-btn:hover),
.dark-mode :deep(.mermaid-zoom-btn:hover){
  background: rgba(70, 70, 70, 1);
  border-color: #4a9eff;
  box-shadow: 0 4px 12px rgba(74, 158, 255, 0.4);
}
body.dark-mode :deep(.mermaid-zoom-btn .zoom-icon),
.dark-mode :deep(.mermaid-zoom-btn .zoom-icon){
  color: #e0e0e0;
}
body.dark-mode :deep(.mermaid-zoom-btn:hover .zoom-icon),
.dark-mode :deep(.mermaid-zoom-btn:hover .zoom-icon){
  color: #4a9eff;
}
:deep(pre:has(code.language-echarts) code),
:deep(pre:has(code.language-mermaid) code){
  opacity: 0;
  position: absolute;
}
:deep(pre:has(code.language-echarts) .copy-btn),
:deep(pre:has(code.language-mermaid) .copy-btn){
  display: none !important;
}
:deep(pre:has(code.language-echarts)),
:deep(pre:has(code.language-mermaid)){
  position: relative;
  min-height: 400px;
  background: var(--background, #ffffff);
  border-radius: 8px;
  overflow: hidden;
  margin: 20px 0;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
:deep(pre:has(code.language-echarts)::before),
:deep(pre:has(code.language-mermaid)::before){
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  z-index: 1;
}
:deep(pre.chart-loading){
  position: relative;
  min-height: 400px;
  background: var(--background, #ffffff);
  border-radius: 8px;
  overflow: hidden;
  margin: 20px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
:deep(pre.chart-loading code){
  display: none !important;
}
:deep(pre.chart-loading::before){
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
:deep(pre.code-loading){
  position: relative;
  min-height: 60px;
  transition: min-height 0.3s ease;
}
:deep(pre.code-loading code){
  opacity: 0.3;
  transition: opacity 0.3s ease;
}
body.dark-mode :deep(pre:has(code.language-echarts)),
body.dark-mode :deep(pre:has(code.language-mermaid)),
body.dark-mode :deep(pre.chart-loading){
  background: rgba(255, 255, 255, 0.03) !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.5) !important;
  border: 1px solid rgba(255, 255, 255, 0.05);
}
body.dark-mode :deep(pre:has(code.language-echarts)::before),
body.dark-mode :deep(pre:has(code.language-mermaid)::before),
body.dark-mode :deep(pre.chart-loading::before){
  border-color: rgba(255, 255, 255, 0.1);
  border-top-color: #409eff;
}
@keyframes spin {
  0% {
    transform: translateX(-50%) rotate(0deg);
  }
  100% {
    transform: translateX(-50%) rotate(360deg);
  }
}
:deep(.echarts-error-message){
  margin: 12px 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(245, 108, 108, 0.08);
  border: 1px solid rgba(245, 108, 108, 0.25);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}
body.dark-mode :deep(.echarts-error-message){
  background: rgba(245, 108, 108, 0.12);
  border-color: rgba(245, 108, 108, 0.35);
}
:deep(.echarts-container){
  position: relative;
  margin: 20px 0;
  padding: 0;
  background: transparent;
  border-radius: 8px;
  overflow: visible;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s ease, transform 0.3s ease;
}
:deep(.echarts-container:hover){
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}
body.dark-mode :deep(.echarts-container){
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.03);
}
body.dark-mode :deep(.echarts-container:hover){
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.6);
  background: rgba(255, 255, 255, 0.05);
}
:deep(.echarts-container canvas){
  display: block;
  border-radius: 8px;
}
:deep(.echarts-container){
  animation: fadeInUp 0.6s ease-out;
}
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

<style>
/* 卡片分享对话框样式（非 scoped）*/
.share-card-dialog.el-dialog {
  --el-dialog-padding-primary: 0;
  background: #f5f4ce !important;
  border-radius: 12px !important;
  padding: 0 !important;
}

.share-card-dialog .el-dialog__header {
  background: #f5f4ce !important;
  border-radius: 12px 12px 0 0 !important;
  padding: 20px 20px 10px !important;
}

.share-card-dialog .el-dialog__body {
  background: #f5f4ce !important;
}

.share-card-dialog .el-dialog__footer {
  background: #f5f4ce !important;
  border-radius: 0 0 12px 12px !important;
  padding: 10px 20px 20px !important;
}

.share-card-dialog .el-dialog__footer .el-button {
  border-radius: 25px;
  padding: 6px 20px;
  font-size: 15px;
  margin-bottom: 20px;
}

/* 文章订阅对话框垂直居中 */
.el-message-box__wrapper {
  align-items: center;
}

.el-message-box {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  margin-top: 0 !important;
}

/* 订阅对话框按钮样式 - 与卡片分享对话框保持一致 */
.el-message-box .el-button {
  border-radius: 25px;
  padding: 6px 20px;
  font-size: 15px;
}

/* ========== 暗色模式适配 - 卡片分享对话框 ========== */
body.dark-mode .share-card-dialog .el-dialog {
  background: #2c2c2c !important;
}

body.dark-mode .share-card-dialog .el-dialog__header {
  background: #2c2c2c !important;
}

body.dark-mode .share-card-dialog .el-dialog__body {
  background: #2c2c2c !important;
}

body.dark-mode .share-card-dialog .el-dialog__footer {
  background: #2c2c2c !important;
}

body.dark-mode .share-card-dialog .el-dialog__title {
  color: #e0e0e0 !important;
}

/* 暗色模式下对话框关闭按钮 */
body.dark-mode .share-card-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #b0b0b0 !important;
}

body.dark-mode
  .share-card-dialog
  .el-dialog__headerbtn:hover
  .el-dialog__close {
  color: #ffffff !important;
}

/* ========== 暗色模式适配 - 文章订阅对话框（MessageBox）========== */
body.dark-mode .el-message-box {
  background-color: #2c2c2c !important;
  border: 1px solid #404040 !important;
}

body.dark-mode .el-message-box__header {
  background-color: #2c2c2c !important;
}

body.dark-mode .el-message-box__title {
  color: #e0e0e0 !important;
}

body.dark-mode .el-message-box__content {
  color: #b0b0b0 !important;
}

body.dark-mode .el-message-box__message {
  color: #b0b0b0 !important;
}

body.dark-mode .el-message-box__headerbtn .el-message-box__close {
  color: #b0b0b0 !important;
}

body.dark-mode .el-message-box__headerbtn:hover .el-message-box__close {
  color: #ffffff !important;
}

/* 订阅对话框按钮暗色模式 */
body.dark-mode .el-message-box .el-button--default {
  background-color: #404040 !important;
  border-color: #505050 !important;
  color: #e0e0e0 !important;
}

body.dark-mode .el-message-box .el-button--default:hover {
  background-color: #505050 !important;
  border-color: #606060 !important;
}

body.dark-mode .el-message-box .el-button--primary {
  background-color: #409eff !important;
  border-color: #409eff !important;
}

body.dark-mode .el-message-box .el-button--primary:hover {
  background-color: #66b1ff !important;
  border-color: #66b1ff !important;
}

/* ========== 暗色模式适配 - 版权声明对话框 ========== */
body.dark-mode .article-copy .el-dialog {
  background-color: #2c2c2c !important;
}

body.dark-mode .article-copy .el-dialog__header {
  background-color: #2c2c2c !important;
}

body.dark-mode .article-copy .el-dialog__title {
  color: #e0e0e0 !important;
}

body.dark-mode .article-copy .el-dialog__body {
  background-color: #2c2c2c !important;
  color: #b0b0b0 !important;
}

body.dark-mode .article-copy .el-dialog__headerbtn .el-dialog__close {
  color: #b0b0b0 !important;
}

body.dark-mode .article-copy .el-dialog__headerbtn:hover .el-dialog__close {
  color: #ffffff !important;
}

/* 版权声明对话框内容适配 */
body.dark-mode .article-copy .copyright-container {
  color: #e0e0e0 !important;
}

body.dark-mode .article-copy .copyright-container p {
  color: #e0e0e0 !important;
}

body.dark-mode .article-copy .copyright-container ul {
  color: #e0e0e0 !important;
}

body.dark-mode .article-copy .copyright-container li {
  color: #e0e0e0 !important;
}

body.dark-mode .article-copy .copyright-container a {
  color: #66b1ff !important;
}

body.dark-mode .article-copy .copyright-container a:hover {
  color: #409eff !important;
}

/* ========== 暗色模式适配 - 通用 el-dialog ========== */
body.dark-mode .el-dialog {
  background-color: #2c2c2c !important;
}

body.dark-mode .el-dialog__header {
  background-color: #2c2c2c !important;
  border-bottom-color: #404040 !important;
}

body.dark-mode .el-dialog__title {
  color: #e0e0e0 !important;
}

body.dark-mode .el-dialog__body {
  background-color: #2c2c2c !important;
  color: #b0b0b0 !important;
}

body.dark-mode .el-dialog__footer {
  background-color: #2c2c2c !important;
  border-top-color: #404040 !important;
}

body.dark-mode .el-dialog__headerbtn .el-dialog__close {
  color: #b0b0b0 !important;
}

body.dark-mode .el-dialog__headerbtn:hover .el-dialog__close {
  color: #ffffff !important;
}

/* 暗色模式下对话框中的输入框 */
body.dark-mode .el-dialog .el-input__inner {
  background-color: #404040 !important;
  border-color: #505050 !important;
  color: #e0e0e0 !important;
}

body.dark-mode .el-dialog .el-input__inner::placeholder {
  color: #888888 !important;
}

body.dark-mode .el-dialog .el-input__inner:focus {
  border-color: #409eff !important;
}

/* 暗色模式下对话框中的按钮 */
body.dark-mode .el-dialog .el-button--default {
  background-color: #404040 !important;
  border-color: #505050 !important;
  color: #e0e0e0 !important;
}

body.dark-mode .el-dialog .el-button--default:hover {
  background-color: #505050 !important;
  border-color: #606060 !important;
}

body.dark-mode .el-dialog .el-button--primary {
  background-color: #409eff !important;
  border-color: #409eff !important;
}

body.dark-mode .el-dialog .el-button--primary:hover {
  background-color: #66b1ff !important;
  border-color: #66b1ff !important;
}

/* Mermaid图表放大overlay */
.mermaid-zoom-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.85);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.3s ease;
}

.mermaid-zoom-content {
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  justify-content: center;
  overflow: auto;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.mermaid-zoom-content svg {
  max-width: 100%;
  max-height: 100%;
  width: auto !important;
  height: auto !important;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.dark-mode .mermaid-zoom-content {
  background: #2d2d2d;
}

.mermaid-zoom-close {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #ddd;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  z-index: 10001;
}

.mermaid-zoom-close:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.1) rotate(90deg);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

.mermaid-zoom-close svg {
  color: #333;
  transition: color 0.2s ease;
}

.mermaid-zoom-close:hover svg {
  color: #ff4444;
}

/* 暗色模式 */
.dark-mode .mermaid-zoom-close {
  background: rgba(45, 45, 45, 0.9);
  border-color: #555;
}

.dark-mode .mermaid-zoom-close:hover {
  background: rgba(45, 45, 45, 1);
}

.dark-mode .mermaid-zoom-close svg {
  color: #ddd;
}

.dark-mode .mermaid-zoom-close:hover svg {
  color: #4a9eff;
}

/* Mermaid 右键菜单样式 */
.mermaid-context-menu {
  position: absolute;
  background: var(--background, #fff);
  border: 1px solid var(--borderColor, #eee);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  padding: 5px 0;
  z-index: 20000;
  min-width: 120px;
}

.mermaid-context-menu .menu-item {
  padding: 8px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--fontColor, #606266);
  transition: background-color 0.2s;
}

.mermaid-context-menu .menu-item:hover {
  background-color: var(--lightGreen);
  color: #fff;
}

.mermaid-context-menu .menu-item .el-icon {
  margin-right: 8px;
  font-size: 16px;
}

body.dark-mode .mermaid-context-menu {
  background: #333;
  border-color: #555;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.5);
}

body.dark-mode .mermaid-context-menu .menu-item {
  color: #eee;
}

body.dark-mode .mermaid-context-menu .menu-item:hover {
  background-color: #444;
  color: #409eff;
}
</style>
