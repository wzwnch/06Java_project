/// <reference types="cypress" />

describe('登录流程测试', () => {
  beforeEach(() => {
    cy.visit('/login')
    cy.clearLocalStorage()
    cy.clearCookies()
  })

  describe('正常登录流程', () => {
    it('应成功显示登录页面', () => {
      cy.get('h2').should('contain', '短链接系统 - 登录')
      cy.get('input[placeholder*="用户名"]').should('exist')
      cy.get('input[placeholder*="密码"]').should('exist')
      cy.get('button').contains('登录').should('exist')
    })

    it('应成功登录并跳转到仪表盘', () => {
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
              phone: '138****8888',
              mail: 't***@test.com',
              realPhone: null,
              realMail: null,
              createTime: '2024-01-01 00:00:00',
              updateTime: '2024-01-01 00:00:00'
            }
          }
        }
      }).as('loginRequest')

      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest').its('request.body').should('deep.equal', {
        username: 'testuser',
        password: 'test123456'
      })

      cy.url().should('include', '/dashboard')
      cy.get('.el-message--success').should('contain', '登录成功')
    })

    it('登录后应正确存储Token和用户信息', () => {
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
              phone: '138****8888',
              mail: 't***@test.com',
              realPhone: null,
              realMail: null,
              createTime: '2024-01-01 00:00:00',
              updateTime: '2024-01-01 00:00:00'
            }
          }
        }
      }).as('loginRequest')

      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest')
      cy.window().then((win) => {
        expect(win.localStorage.getItem('token')).to.equal('test-token-123')
      })
    })

    it('登录后应跳转到原请求页面', () => {
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

      cy.visit('/login?redirect=/link')
      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest')
      cy.url().should('include', '/link')
    })
  })

  describe('异常登录流程', () => {
    it('用户名为空应显示错误提示', () => {
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入用户名')
    })

    it('密码为空应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('button').contains('登录').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入密码')
    })

    it('用户名长度不足应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('abc')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()
      
      cy.get('.el-form-item__error').should('contain', '用户名长度为4-20个字符')
    })

    it('用户名长度超限应显示错误提示', () => {
      const longUsername = 'a'.repeat(21)
      cy.get('input[placeholder*="用户名"]').type(longUsername)
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()
      
      cy.get('.el-form-item__error').should('contain', '用户名长度为4-20个字符')
    })

    it('用户名包含非法字符应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('test@user')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()
      
      cy.get('.el-form-item__error').should('contain', '用户名只能包含字母、数字和下划线')
    })

    it('密码长度不足应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('12345')
      cy.get('button').contains('登录').click()
      
      cy.get('.el-form-item__error').should('contain', '密码长度为6-20个字符')
    })

    it('密码长度超限应显示错误提示', () => {
      const longPassword = 'a'.repeat(21)
      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type(longPassword)
      cy.get('button').contains('登录').click()
      
      cy.get('.el-form-item__error').should('contain', '密码长度为6-20个字符')
    })

    it('用户名不存在应显示错误提示', () => {
      cy.intercept('POST', '/api/user/login', {
        statusCode: 200,
        body: {
          code: 1005,
          msg: '用户名或密码错误',
          data: null
        }
      }).as('loginRequest')

      cy.get('input[placeholder*="用户名"]').type('nonexistent')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest')
      cy.get('.el-message--error').should('contain', '用户名或密码错误')
    })

    it('密码错误应显示错误提示', () => {
      cy.intercept('POST', '/api/user/login', {
        statusCode: 200,
        body: {
          code: 1005,
          msg: '用户名或密码错误',
          data: null
        }
      }).as('loginRequest')

      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('wrongpassword')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest')
      cy.get('.el-message--error').should('contain', '用户名或密码错误')
    })

    it('Token过期应跳转登录页', () => {
      cy.intercept('POST', '/api/user/login', {
        statusCode: 401,
        body: {
          code: 401,
          msg: '登录已过期，请重新登录',
          data: null
        }
      }).as('loginRequest')

      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest')
      cy.get('.el-message--error').should('contain', '登录已过期')
    })

    it('网络异常应显示错误提示', () => {
      cy.intercept('POST', '/api/user/login', {
        forceNetworkError: true
      }).as('loginRequest')

      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest')
      cy.get('.el-message--error').should('exist')
    })

    it('服务器错误应显示错误提示', () => {
      cy.intercept('POST', '/api/user/login', {
        statusCode: 500,
        body: {
          code: 500,
          msg: '服务器内部错误',
          data: null
        }
      }).as('loginRequest')

      cy.get('input[placeholder*="用户名"]').type('testuser')
      cy.get('input[placeholder*="密码"]').type('test123456')
      cy.get('button').contains('登录').click()

      cy.wait('@loginRequest')
      cy.get('.el-message--error').should('contain', '服务器内部错误')
    })
  })

  describe('登录页面导航测试', () => {
    it('点击注册链接应跳转到注册页面', () => {
      cy.contains('立即注册').click()
      cy.url().should('include', '/register')
    })
  })
})
