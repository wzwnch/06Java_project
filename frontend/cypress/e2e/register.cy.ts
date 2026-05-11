/// <reference types="cypress" />

describe('注册流程测试', () => {
  beforeEach(() => {
    cy.visit('/register')
    cy.clearLocalStorage()
    cy.clearCookies()
  })

  describe('正常注册流程', () => {
    it('应成功显示注册页面', () => {
      cy.get('h2').should('contain', '短链接系统 - 注册')
      cy.get('input[placeholder*="用户名"]').should('exist')
      cy.get('input[placeholder*="密码"]').should('exist')
      cy.get('input[placeholder*="确认密码"]').should('exist')
      cy.get('input[placeholder*="手机号"]').should('exist')
      cy.get('input[placeholder*="邮箱"]').should('exist')
      cy.get('button').contains('注册').should('exist')
    })

    it('应成功注册并跳转到登录页面', () => {
      cy.intercept('POST', '/api/user/register', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('registerRequest')

      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()

      cy.wait('@registerRequest').its('request.body').should('deep.include', {
        username: 'newuser',
        password: 'test123456'
      })

      cy.url().should('include', '/login')
      cy.get('.el-message--success').should('contain', '注册成功')
    })

    it('带手机号和邮箱注册应成功', () => {
      cy.intercept('POST', '/api/user/register', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('registerRequest')

      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('input[placeholder*="手机号"]').type('13812345678')
      cy.get('input[placeholder*="邮箱"]').type('test@example.com')
      cy.get('button').contains('注册').click()

      cy.wait('@registerRequest').its('request.body').should('deep.equal', {
        username: 'newuser',
        password: 'test123456',
        phone: '13812345678',
        mail: 'test@example.com'
      })

      cy.url().should('include', '/login')
    })
  })

  describe('异常注册流程', () => {
    it('用户名为空应显示错误提示', () => {
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入用户名')
    })

    it('用户名长度不足应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('abc')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '用户名长度为4-20个字符')
    })

    it('用户名长度超限应显示错误提示', () => {
      const longUsername = 'a'.repeat(21)
      cy.get('input[placeholder*="用户名"]').type(longUsername)
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '用户名长度为4-20个字符')
    })

    it('用户名包含非法字符应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('test@user')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '用户名只能包含字母、数字和下划线')
    })

    it('密码为空应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入密码')
    })

    it('密码长度不足应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('12345')
      cy.get('input[placeholder*="确认密码"]').type('12345')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '密码长度为6-20个字符')
    })

    it('密码长度超限应显示错误提示', () => {
      const longPassword = 'a'.repeat(21)
      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type(longPassword)
      cy.get('input[placeholder*="确认密码"]').type(longPassword)
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '密码长度为6-20个字符')
    })

    it('确认密码为空应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '请确认密码')
    })

    it('两次密码不一致应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test654321')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '两次输入的密码不一致')
    })

    it('手机号格式不正确应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('input[placeholder*="手机号"]').type('12345678901')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入正确的手机号')
    })

    it('邮箱格式不正确应显示错误提示', () => {
      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('input[placeholder*="邮箱"]').type('invalid-email')
      cy.get('button').contains('注册').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入正确的邮箱地址')
    })

    it('用户名已存在应显示错误提示', () => {
      cy.intercept('POST', '/api/user/register', {
        statusCode: 200,
        body: {
          code: 1001,
          msg: '用户名已被注册',
          data: null
        }
      }).as('registerRequest')

      cy.get('input[placeholder*="用户名"]').type('existinguser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()

      cy.wait('@registerRequest')
      cy.get('.el-message--error').should('contain', '用户名已被注册')
    })

    it('手机号已绑定应显示错误提示', () => {
      cy.intercept('POST', '/api/user/register', {
        statusCode: 200,
        body: {
          code: 1004,
          msg: '手机号已被使用',
          data: null
        }
      }).as('registerRequest')

      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('input[placeholder*="手机号"]').type('13812345678')
      cy.get('button').contains('注册').click()

      cy.wait('@registerRequest')
      cy.get('.el-message--error').should('contain', '手机号已被使用')
    })

    it('邮箱已绑定应显示错误提示', () => {
      cy.intercept('POST', '/api/user/register', {
        statusCode: 200,
        body: {
          code: 1004,
          msg: '邮箱已被使用',
          data: null
        }
      }).as('registerRequest')

      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('input[placeholder*="邮箱"]').type('existing@example.com')
      cy.get('button').contains('注册').click()

      cy.wait('@registerRequest')
      cy.get('.el-message--error').should('contain', '邮箱已被使用')
    })

    it('网络异常应显示错误提示', () => {
      cy.intercept('POST', '/api/user/register', {
        forceNetworkError: true
      }).as('registerRequest')

      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()

      cy.wait('@registerRequest')
      cy.get('.el-message--error').should('exist')
    })

    it('服务器错误应显示错误提示', () => {
      cy.intercept('POST', '/api/user/register', {
        statusCode: 500,
        body: {
          code: 500,
          msg: '服务器内部错误',
          data: null
        }
      }).as('registerRequest')

      cy.get('input[placeholder*="用户名"]').type('newuser')
      cy.get('input[placeholder*="密码"]').eq(0).type('test123456')
      cy.get('input[placeholder*="确认密码"]').type('test123456')
      cy.get('button').contains('注册').click()

      cy.wait('@registerRequest')
      cy.get('.el-message--error').should('contain', '服务器内部错误')
    })
  })

  describe('注册页面导航测试', () => {
    it('点击登录链接应跳转到登录页面', () => {
      cy.contains('立即登录').click()
      cy.url().should('include', '/login')
    })
  })
})
