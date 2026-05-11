/// <reference types="cypress" />

describe('回收站操作流程测试', () => {
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

  function mockRecycleList(): void {
    cy.intercept('GET', '/api/recycle/page*', {
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
              createTime: '2024-01-01 00:00:00',
              updateTime: '2024-01-02 00:00:00'
            }
          ],
          total: 1,
          current: 1,
          size: 10
        }
      }
    }).as('recycleListRequest')
  }

  function mockEmptyRecycleList(): void {
    cy.intercept('GET', '/api/recycle/page*', {
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
    }).as('emptyRecycleListRequest')
  }

  describe('正常回收站操作流程', () => {
    it('应成功显示回收站页面', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('h2').should('contain', '回收站')
    })

    it('应成功显示回收站列表', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('.el-table__body tr').should('have.length', 1)
      cy.get('.el-table__body').should('contain', 'abc123')
    })

    it('应成功恢复短链接', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.intercept('PUT', '/api/recycle/recover', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('recoverRequest')

      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('button').contains('恢复').click()
      cy.get('.el-message-box').should('be.visible')
      cy.get('.el-message-box__btns button').contains('确定').click()
      
      cy.wait('@recoverRequest').its('request.body').should('deep.equal', {
        shortCode: 'abc123'
      })
      
      cy.get('.el-message--success').should('contain', '恢复短链接成功')
    })

    it('应成功彻底删除短链接', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.intercept('DELETE', '/api/recycle/abc123', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('removeRequest')

      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('button').contains('彻底删除').click()
      cy.get('.el-message-box').should('be.visible')
      cy.get('.el-message-box__btns button').contains('确定删除').click()
      
      cy.wait('@removeRequest')
      cy.get('.el-message--success').should('contain', '彻底删除成功')
    })

    it('回收站为空应显示空状态', () => {
      login()
      mockGroupList()
      mockEmptyRecycleList()
      
      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@emptyRecycleListRequest'])
      
      cy.get('.el-empty').should('be.visible')
      cy.get('.el-empty__description').should('contain', '回收站为空')
    })

    it('应成功按分组筛选', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.intercept('GET', '/api/recycle/page*gid=default*', {
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
      }).as('filterRequest')

      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('.filter-bar .el-select').click()
      cy.get('.el-select-dropdown__item').contains('默认分组').click()
      
      cy.wait('@filterRequest')
    })

    it('应成功分页查询', () => {
      login()
      mockGroupList()
      
      cy.intercept('GET', '/api/recycle/page*', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            records: Array(10).fill(null).map((_, i) => ({
              shortCode: `code${i}`,
              shortUrl: `http://localhost:8080/code${i}`,
              originUrl: 'https://www.example.com',
              gid: 'default',
              title: `Link ${i}`,
              faviconUrl: null,
              expireTime: null,
              createTime: '2024-01-01 00:00:00',
              updateTime: '2024-01-02 00:00:00'
            })),
            total: 20,
            current: 1,
            size: 10
          }
        }
      }).as('recycleListRequest')

      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('.el-pagination').should('exist')
      cy.get('.el-pagination__total').should('contain', '20')
    })
  })

  describe('异常回收站操作流程', () => {
    it('恢复时短链接码已被占用应显示错误提示', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.intercept('PUT', '/api/recycle/recover', {
        statusCode: 200,
        body: {
          code: 4001,
          msg: '短链接码已被占用',
          data: null
        }
      }).as('recoverRequest')

      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('button').contains('恢复').click()
      cy.get('.el-message-box__btns button').contains('确定').click()
      
      cy.wait('@recoverRequest')
      cy.get('.el-message--error').should('contain', '短链接码已被占用')
    })

    it('彻底删除不存在的短链接应显示错误提示', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.intercept('DELETE', '/api/recycle/abc123', {
        statusCode: 200,
        body: {
          code: 4002,
          msg: '短链接不存在',
          data: null
        }
      }).as('removeRequest')

      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('button').contains('彻底删除').click()
      cy.get('.el-message-box__btns button').contains('确定删除').click()
      
      cy.wait('@removeRequest')
      cy.get('.el-message--error').should('contain', '短链接不存在')
    })

    it('网络异常应显示错误提示', () => {
      login()
      
      cy.intercept('GET', '/api/group/list', {
        forceNetworkError: true
      }).as('groupListRequest')

      cy.intercept('GET', '/api/recycle/page*', {
        forceNetworkError: true
      }).as('recycleListRequest')

      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('.el-message--error').should('exist')
    })

    it('取消恢复操作应不执行恢复', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('button').contains('恢复').click()
      cy.get('.el-message-box__btns button').contains('取消').click()
      
      cy.get('.el-message-box').should('not.exist')
    })

    it('取消彻底删除操作应不执行删除', () => {
      login()
      mockGroupList()
      mockRecycleList()
      
      cy.visit('/recycle')
      cy.wait(['@groupListRequest', '@recycleListRequest'])
      
      cy.get('button').contains('彻底删除').click()
      cy.get('.el-message-box__btns button').contains('取消').click()
      
      cy.get('.el-message-box').should('not.exist')
    })
  })
})
