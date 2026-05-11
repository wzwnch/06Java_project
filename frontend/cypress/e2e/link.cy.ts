/// <reference types="cypress" />

describe('短链接创建流程测试', () => {
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
          { gid: 'default', name: '默认分组', sortOrder: 0 },
          { gid: 'work', name: '工作', sortOrder: 1 }
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
          records: [],
          total: 0,
          current: 1,
          size: 10
        }
      }
    }).as('linkListRequest')
  }

  describe('正常创建流程', () => {
    it('应成功显示短链接管理页面', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('h2').should('contain', '短链接管理')
      cy.get('button').contains('新增短链接').should('exist')
    })

    it('应成功创建短链接', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.intercept('POST', '/api/link', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            shortCode: 'abc123',
            shortUrl: 'http://localhost:8080/abc123',
            originUrl: 'https://www.example.com',
            gid: 'default',
            title: 'Example Domain',
            faviconUrl: null,
            expireTime: null,
            createTime: '2024-01-01 00:00:00'
          }
        }
      }).as('createLinkRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('.el-dialog').should('be.visible')
      cy.get('.el-dialog__title').should('contain', '新增短链接')
      
      cy.get('input[placeholder*="目标URL"]').type('https://www.example.com')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@createLinkRequest').its('request.body').should('deep.include', {
        originUrl: 'https://www.example.com'
      })
      
      cy.get('.el-message--success').should('contain', '新增短链接成功')
    })

    it('带自定义短码创建应成功', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.intercept('POST', '/api/link', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            shortCode: 'mycode',
            shortUrl: 'http://localhost:8080/mycode',
            originUrl: 'https://www.example.com',
            gid: 'default',
            title: 'Example Domain',
            faviconUrl: null,
            expireTime: null,
            createTime: '2024-01-01 00:00:00'
          }
        }
      }).as('createLinkRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('input[placeholder*="目标URL"]').type('https://www.example.com')
      cy.get('input[placeholder*="自定义短码"]').type('mycode')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@createLinkRequest').its('request.body').should('deep.include', {
        originUrl: 'https://www.example.com',
        customCode: 'mycode'
      })
    })

    it('带有效期创建应成功', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.intercept('POST', '/api/link', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: {
            shortCode: 'abc123',
            shortUrl: 'http://localhost:8080/abc123',
            originUrl: 'https://www.example.com',
            gid: 'default',
            title: 'Example Domain',
            faviconUrl: null,
            expireTime: '2024-12-31 23:59:59',
            createTime: '2024-01-01 00:00:00'
          }
        }
      }).as('createLinkRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('input[placeholder*="目标URL"]').type('https://www.example.com')
      cy.get('.el-date-editor input').click()
      cy.get('.el-picker-panel').should('be.visible')
      cy.get('.el-dialog button').contains('确定').click()
    })
  })

  describe('异常创建流程', () => {
    it('目标URL为空应显示错误提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入目标URL')
    })

    it('目标URL格式不合法应显示错误提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('input[placeholder*="目标URL"]').type('invalid-url')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.get('.el-form-item__error').should('contain', 'URL格式不合法')
    })

    it('自定义短码格式不合法应显示错误提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('input[placeholder*="目标URL"]').type('https://www.example.com')
      cy.get('input[placeholder*="自定义短码"]').type('test@123')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.get('.el-form-item__error').should('contain', '自定义短链接码只能包含字母和数字')
    })

    it('分组不存在应显示错误提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.intercept('POST', '/api/link', {
        statusCode: 200,
        body: {
          code: 3001,
          msg: '分组不存在',
          data: null
        }
      }).as('createLinkRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('input[placeholder*="目标URL"]').type('https://www.example.com')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@createLinkRequest')
      cy.get('.el-message--error').should('contain', '分组不存在')
    })

    it('自定义短码已存在应显示错误提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.intercept('POST', '/api/link', {
        statusCode: 200,
        body: {
          code: 2001,
          msg: '短链接码已存在',
          data: null
        }
      }).as('createLinkRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('input[placeholder*="目标URL"]').type('https://www.example.com')
      cy.get('input[placeholder*="自定义短码"]').type('existing')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@createLinkRequest')
      cy.get('.el-message--error').should('contain', '短链接码已存在')
    })

    it('网络异常应显示错误提示', () => {
      login()
      mockGroupList()
      mockLinkList()
      
      cy.intercept('POST', '/api/link', {
        forceNetworkError: true
      }).as('createLinkRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('新增短链接').click()
      cy.get('input[placeholder*="目标URL"]').type('https://www.example.com')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@createLinkRequest')
      cy.get('.el-message--error').should('exist')
    })
  })

  describe('短链接列表操作测试', () => {
    it('应正确显示短链接列表', () => {
      login()
      mockGroupList()
      
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
            size: 10
          }
        }
      }).as('linkListRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('.el-table__body tr').should('have.length', 1)
      cy.get('.el-table__body').should('contain', 'abc123')
    })

    it('应成功复制短链接', () => {
      login()
      mockGroupList()
      
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
            size: 10
          }
        }
      }).as('linkListRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('复制').click()
      cy.get('.el-message--success').should('contain', '已复制')
    })

    it('应成功删除短链接', () => {
      login()
      mockGroupList()
      
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
            size: 10
          }
        }
      }).as('linkListRequest')

      cy.intercept('DELETE', '/api/link/abc123', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('deleteLinkRequest')

      cy.visit('/link')
      cy.wait(['@groupListRequest', '@linkListRequest'])
      
      cy.get('button').contains('删除').click()
      cy.get('.el-message-box').should('be.visible')
      cy.get('.el-message-box__btns button').contains('确定').click()
      
      cy.wait('@deleteLinkRequest')
      cy.get('.el-message--success').should('contain', '删除短链接成功')
    })
  })
})
