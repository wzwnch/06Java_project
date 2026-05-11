describe('监控统计展示测试', () => {
  const testUser = {
    username: 'testuser',
    password: 'test123456'
  }

  beforeEach(() => {
    cy.clearLocalStorage()
    cy.clearCookies()
  })

  function login(): void {
    cy.intercept('POST', '/api/user/login', {
      statusCode: 200,
      body: {
        code: 0,
        msg: 'success',
        data: {
          token: 'test-token-123',
          userInfo: {
            id: 1,
            username: 'testuser',
            phone: null,
            mail: null,
            realPhone: null,
            realMail: null,
            createTime: '2024-01-01 00:00:00',
            updateTime: '2024-01-01 00:00:00'
          }
        }
      }
    }).as('loginRequest')

    cy.visit('/login')
    cy.get('input[placeholder*="用户名"]').type(testUser.username)
    cy.get('input[placeholder*="密码"]').type(testUser.password)
    cy.get('button').contains('登录').click()
    cy.wait('@loginRequest')
  }

  function mockGroupList(): void {
    cy.intercept('GET', '/api/group/list', {
      statusCode: 200,
      body: {
        code: 0,
        msg: 'success',
        data: [
          { gid: 'default', name: '默认分组', sortOrder: 0, createTime: '2024-01-01 00:00:00', updateTime: '2024-01-01 00:00:00' }
        ]
      }
    }).as('groupListRequest')
  }

  function mockLinkList(): void {
    cy.intercept('GET', '/api/link/page*', {
      statusCode: 200,
      body: {
        code: 0,
        msg: 'success',
        data: {
          records: [
            {
              shortCode: 'abc123',
              shortUrl: 'http://localhost:8080/abc123',
              originUrl: 'https://www.example.com',
              gid: 'default',
              title: 'Example',
              faviconUrl: null,
              expireTime: null,
              createTime: '2024-01-01 00:00:00'
            }
          ],
          total: 1,
          current: 1,
          size: 100
        }
      }
    }).as('linkListRequest')
  }

  function mockTodayStats(): void {
    cy.intercept('GET', '/api/stats/today*', {
      statusCode: 200,
      body: {
        code: 0,
        msg: 'success',
        data: {
          date: '2024-01-01',
          pv: 1000,
          uv: 500,
          uip: 300
        }
      }
    }).as('todayStatsRequest')
  }

  function mockLinkStats(): void {
    cy.intercept('GET', '/api/stats/link/abc123', {
      statusCode: 200,
      body: {
        code: 0,
        msg: 'success',
        data: {
          shortCode: 'abc123',
          gid: 'default',
          pv: 10000,
          uv: 5000,
          uip: 3000
        }
      }
    }).as('linkStatsRequest')
  }

  function mockHistoryStats(): void {
    cy.intercept('GET', '/api/stats/history*', {
      statusCode: 200,
      body: {
        code: 0,
        msg: 'success',
        data: [
          { date: '2024-01-01', pv: 100, uv: 50, uip: 30 },
          { date: '2024-01-02', pv: 120, uv: 60, uip: 40 },
          { date: '2024-01-03', pv: 150, uv: 70, uip: 50 }
        ]
      }
    }).as('historyStatsRequest')
  }

  function mockAccessLog(): void {
    cy.intercept('GET', '/api/stats/log/page*', {
      statusCode: 200,
      body: {
        code: 0,
        msg: 'success',
        data: {
          records: [
            {
              id: 1,
              shortCode: 'abc123',
              gid: 'default',
              pv: 1,
              uv: 'uv-123',
              uip: '192.168.1.1',
              ip: '192.168.1.1',
              region: '北京',
              os: 'Windows',
              browser: 'Chrome',
              device: 'PC',
              network: '电信',
              createTime: '2024-01-01 12:00:00'
            }
          ],
          total: 1,
          current: 1,
          size: 10
        }
      }
    }).as('accessLogRequest')
  }

  describe('正常监控统计展示流程', () => {
    it('应成功显示监控统计页面', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('h2').should('contain', '监控统计')
    })

    it('未选择短链接应显示提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.el-empty').should('be.visible')
      cy.get('.el-empty__description').should('contain', '请先选择一个短链接')
    })

    it('选择短链接后应显示统计数据', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.stat-card').should('have.length', 4)
    })

    it('应正确显示今日PV统计', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.stat-card').first().should('contain', '今日 PV')
      cy.get('.stat-card').first().should('contain', '1,000')
    })

    it('应正确显示今日UV统计', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.stat-card').eq(1).should('contain', '今日 UV')
      cy.get('.stat-card').eq(1).should('contain', '500')
    })

    it('应正确显示今日UIP统计', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.stat-card').eq(2).should('contain', '今日 UIP')
      cy.get('.stat-card').eq(2).should('contain', '300')
    })

    it('应正确显示累计PV统计', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.stat-card').eq(3).should('contain', '累计 PV')
      cy.get('.stat-card').eq(3).should('contain', '10,000')
    })

    it('应正确显示历史趋势图表', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.chart-card').should('contain', '历史趋势')
    })

    it('应正确显示访问日志列表', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.log-card').should('contain', '访问日志')
      cy.get('.log-table').should('contain', '192.168.1.1')
      cy.get('.log-table').should('contain', '北京')
    })

    it('应正确显示地区分布图表', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.pie-card').first().should('contain', '地区分布')
    })

    it('应正确显示设备分布图表', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.pie-card').eq(1).should('contain', '设备分布')
    })

    it('应正确显示浏览器分布图表', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.pie-card').eq(2).should('contain', '浏览器分布')
    })

    it('应正确显示操作系统分布图表', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      mockAccessLog()
      
      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.pie-card').eq(3).should('contain', '操作系统分布')
    })
  })

  describe('异常监控统计展示流程', () => {
    it('无访问数据应显示零值', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.intercept('GET', '/api/stats/today*', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            date: '2024-01-01',
            pv: 0,
            uv: 0,
            uip: 0
          }
        }
      }).as('todayStatsRequest')
      
      cy.intercept('GET', '/api/stats/link/abc123', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            shortCode: 'abc123',
            gid: 'default',
            pv: 0,
            uv: 0,
            uip: 0
          }
        }
      }).as('linkStatsRequest')
      
      cy.intercept('GET', '/api/stats/history*', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: []
        }
      }).as('historyStatsRequest')
      
      cy.intercept('GET', '/api/stats/log/page*', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            records: [],
            total: 0,
            current: 1,
            size: 10
          }
        }
      }).as('accessLogRequest')

      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.stat-value').should('contain', '0')
    })

    it('IP解析失败应显示未知', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      mockHistoryStats()
      
      cy.intercept('GET', '/api/stats/log/page*', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            records: [
              {
                id: 1,
                shortCode: 'abc123',
                gid: 'default',
                pv: 1,
                uv: 'uv-123',
                uip: '0.0.0.0',
                ip: '0.0.0.0',
                region: null,
                os: 'Unknown',
                browser: 'Unknown',
                device: 'Unknown',
                network: null,
                createTime: '2024-01-01 12:00:00'
              }
            ],
            total: 1,
            current: 1,
            size: 10
          }
        }
      }).as('accessLogRequest')

      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.log-table').should('contain', '未知')
    })

    it('时间范围超过限制应显示错误提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      mockTodayStats()
      mockLinkStats()
      
      cy.intercept('GET', '/api/stats/history*', {
        statusCode: 200,
        body: {
          code: 5001,
          msg: '时间范围超过限制，最多查询30天',
          data: null
        }
      }).as('historyStatsRequest')
      
      mockAccessLog()

      cy.visit('/stats')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.filter-bar .el-select').eq(1).click()
      cy.get('.el-select-dropdown__item').contains('abc123').click()
      cy.wait(['@todayStatsRequest', '@linkStatsRequest', '@historyStatsRequest', '@accessLogRequest'])
      
      cy.get('.el-message--error').should('contain', '时间范围超过限制')
    })

    it('网络异常应显示错误提示', () => {
      login()
      
      cy.intercept('GET', '/api/group/list', {
        forceNetworkError: true
      }).as('groupListRequest')

      cy.visit('/stats')
      cy.wait('@groupListRequest')
      
      cy.get('.el-message--error').should('exist')
    })
  })
})
