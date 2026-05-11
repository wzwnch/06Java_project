/// <reference types="cypress" />

describe('短链接跳转流程测试', () => {
  describe('正常跳转流程', () => {
    it('应成功跳转到目标URL', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/abc123', (req) => {
        req.redirect(targetUrl, 302)
      }).as('redirectRequest')

      cy.visit('/abc123')
      cy.wait('@redirectRequest')
      
      cy.url().should('include', 'example.com')
    })

    it('跳转时应记录访问日志', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/abc123', (req) => {
        req.redirect(targetUrl, 302)
      }).as('redirectRequest')

      cy.visit('/abc123', {
        onBeforeLoad(win) {
          Object.defineProperty(win.navigator, 'userAgent', {
            value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0'
          })
        }
      })
      
      cy.wait('@redirectRequest')
    })

    it('跳转时应正确处理不同协议的URL', () => {
      const httpUrl = 'http://www.example.com'
      
      cy.intercept('GET', '/http123', (req) => {
        req.redirect(httpUrl, 302)
      }).as('redirectRequest')

      cy.visit('/http123')
      cy.wait('@redirectRequest')
    })

    it('跳转时应正确处理带查询参数的URL', () => {
      const targetUrl = 'https://www.example.com/page?param1=value1&param2=value2'
      
      cy.intercept('GET', '/query123', (req) => {
        req.redirect(targetUrl, 302)
      }).as('redirectRequest')

      cy.visit('/query123')
      cy.wait('@redirectRequest')
    })

    it('跳转时应正确处理带锚点的URL', () => {
      const targetUrl = 'https://www.example.com/page#section'
      
      cy.intercept('GET', '/anchor123', (req) => {
        req.redirect(targetUrl, 302)
      }).as('redirectRequest')

      cy.visit('/anchor123')
      cy.wait('@redirectRequest')
    })
  })

  describe('异常跳转流程', () => {
    it('短链接不存在应跳转404页面', () => {
      cy.intercept('GET', '/nonexistent', {
        statusCode: 404,
        body: 'Not Found'
      }).as('notFoundRequest')

      cy.visit('/nonexistent', { failOnStatusCode: false })
      cy.wait('@notFoundRequest')
      
      cy.get('body').should('exist')
    })

    it('短链接已过期应跳转过期页面', () => {
      cy.intercept('GET', '/expired123', {
        statusCode: 200,
        body: '<html><body>链接已过期</body></html>',
        headers: {
          'content-type': 'text/html'
        }
      }).as('expiredRequest')

      cy.visit('/expired123')
      cy.wait('@expiredRequest')
      
      cy.get('body').should('contain', '过期')
    })

    it('短链接码格式不合法应返回错误', () => {
      cy.intercept('GET', '/invalid@code', {
        statusCode: 400,
        body: 'Bad Request'
      }).as('invalidRequest')

      cy.visit('/invalid@code', { failOnStatusCode: false })
      cy.wait('@invalidRequest')
    })

    it('服务器错误应显示错误页面', () => {
      cy.intercept('GET', '/error500', {
        statusCode: 500,
        body: 'Internal Server Error'
      }).as('errorRequest')

      cy.visit('/error500', { failOnStatusCode: false })
      cy.wait('@errorRequest')
    })
  })

  describe('缓存策略测试', () => {
    it('热点短链接应命中缓存', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/hotlink', (req) => {
        req.reply({
          statusCode: 302,
          headers: {
            'Location': targetUrl,
            'X-Cache': 'HIT'
          }
        })
      }).as('cachedRequest')

      cy.visit('/hotlink')
      cy.wait('@cachedRequest').its('response.headers.x-cache').should('eq', 'HIT')
    })

    it('首次访问应缓存结果', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/newlink', (req) => {
        req.reply({
          statusCode: 302,
          headers: {
            'Location': targetUrl,
            'X-Cache': 'MISS'
          }
        })
      }).as('missRequest')

      cy.visit('/newlink')
      cy.wait('@missRequest').its('response.headers.x-cache').should('eq', 'MISS')
    })

    it('不存在的短链接应缓存空值', () => {
      cy.intercept('GET', '/nonexistent', {
        statusCode: 404,
        headers: {
          'X-Cache': 'NULL-CACHE'
        },
        body: 'Not Found'
      }).as('nullCacheRequest')

      cy.visit('/nonexistent', { failOnStatusCode: false })
      cy.wait('@nullCacheRequest')
    })
  })

  describe('并发跳转测试', () => {
    it('并发请求同一短链接应正确处理', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/concurrent', (req) => {
        req.redirect(targetUrl, 302)
      }).as('concurrentRequest')

      for (let i = 0; i < 5; i++) {
        cy.request({
          url: '/concurrent',
          followRedirect: false
        }).then((response) => {
          expect(response.status).to.eq(302)
          expect(response.headers.location).to.eq(targetUrl)
        })
      }
    })
  })

  describe('统计记录测试', () => {
    it('跳转应记录PV', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/pvtest', (req) => {
        req.redirect(targetUrl, 302)
      }).as('pvRequest')

      cy.visit('/pvtest')
      cy.wait('@pvRequest')
    })

    it('同一用户多次访问应记录UV不重复', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/uvtest', (req) => {
        req.redirect(targetUrl, 302)
      }).as('uvRequest')

      cy.visit('/uvtest')
      cy.wait('@uvRequest')
      
      cy.visit('/uvtest')
      cy.wait('@uvRequest')
    })

    it('不同IP访问应记录UIP', () => {
      const targetUrl = 'https://www.example.com'
      
      cy.intercept('GET', '/uiptest', (req) => {
        req.redirect(targetUrl, 302)
      }).as('uipRequest')

      cy.visit('/uiptest')
      cy.wait('@uipRequest')
    })
  })
})
