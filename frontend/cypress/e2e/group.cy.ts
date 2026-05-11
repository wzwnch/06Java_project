/// <reference types="cypress" />

describe('分组管理流程测试', () => {
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
          { gid: 'default', name: '默认分组', sortOrder: 0, createTime: '2024-01-01 00:00:00', updateTime: '2024-01-01 00:00:00' },
          { gid: 'work', name: '工作', sortOrder: 1, createTime: '2024-01-01 00:00:00', updateTime: '2024-01-01 00:00:00' }
        ]
      }
    }).as('groupListRequest')
  }

  describe('正常分组管理流程', () => {
    it('应成功显示分组管理页面', () => {
      login()
      mockGroupList()
      
      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('h2').should('contain', '分组管理')
      cy.get('button').contains('新增分组').should('exist')
    })

    it('应成功显示分组列表', () => {
      login()
      mockGroupList()
      
      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').should('have.length', 2)
      cy.get('.el-table__body').should('contain', '默认分组')
      cy.get('.el-table__body').should('contain', '工作')
    })

    it('应成功创建分组', () => {
      login()
      mockGroupList()
      
      cy.intercept('POST', '/api/group', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('createGroupRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('button').contains('新增分组').click()
      cy.get('.el-dialog').should('be.visible')
      cy.get('.el-dialog__title').should('contain', '新增分组')
      
      cy.get('input[placeholder*="分组名称"]').type('新分组')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@createGroupRequest').its('request.body').should('deep.include', {
        name: '新分组'
      })
      
      cy.get('.el-message--success').should('contain', '新增分组成功')
    })

    it('应成功修改分组', () => {
      login()
      mockGroupList()
      
      cy.intercept('PUT', '/api/group', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('updateGroupRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').first().find('button').contains('编辑').click()
      cy.get('.el-dialog').should('be.visible')
      
      cy.get('input[placeholder*="分组名称"]').clear().type('修改后的分组名')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@updateGroupRequest').its('request.body').should('deep.include', {
        name: '修改后的分组名'
      })
      
      cy.get('.el-message--success').should('contain', '修改分组成功')
    })

    it('应成功删除分组', () => {
      login()
      mockGroupList()
      
      cy.intercept('DELETE', '/api/group/work', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('deleteGroupRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').eq(1).find('button').contains('删除').click()
      cy.get('.el-message-box').should('be.visible')
      cy.get('.el-message-box__btns button').contains('确定').click()
      
      cy.wait('@deleteGroupRequest')
      cy.get('.el-message--success').should('contain', '删除分组成功')
    })

    it('应成功上移分组', () => {
      login()
      mockGroupList()
      
      cy.intercept('PUT', '/api/group/sort', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('sortGroupRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').eq(1).find('button').contains('上移').click()
      
      cy.wait('@sortGroupRequest').its('request.body.gidList').should('deep.equal', ['work', 'default'])
      
      cy.get('.el-message--success').should('contain', '排序保存成功')
    })

    it('应成功下移分组', () => {
      login()
      mockGroupList()
      
      cy.intercept('PUT', '/api/group/sort', {
        statusCode: 200,
        body: {
          code: 0,
          msg: 'success',
          data: null
        }
      }).as('sortGroupRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').first().find('button').contains('下移').click()
      
      cy.wait('@sortGroupRequest').its('request.body.gidList').should('deep.equal', ['work', 'default'])
      
      cy.get('.el-message--success').should('contain', '排序保存成功')
    })

    it('第一个分组上移按钮应禁用', () => {
      login()
      mockGroupList()
      
      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').first().find('button').contains('上移').should('be.disabled')
    })

    it('最后一个分组下移按钮应禁用', () => {
      login()
      mockGroupList()
      
      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').last().find('button').contains('下移').should('be.disabled')
    })
  })

  describe('异常分组管理流程', () => {
    it('分组名称为空应显示错误提示', () => {
      login()
      mockGroupList()
      
      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('button').contains('新增分组').click()
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.get('.el-form-item__error').should('contain', '请输入分组名称')
    })

    it('分组名称超长应显示错误提示', () => {
      login()
      mockGroupList()
      
      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('button').contains('新增分组').click()
      
      const longName = 'a'.repeat(65)
      cy.get('input[placeholder*="分组名称"]').type(longName)
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.get('.el-form-item__error').should('contain', '分组名称不能超过64个字符')
    })

    it('分组名称重复应显示错误提示', () => {
      login()
      mockGroupList()
      
      cy.intercept('POST', '/api/group', {
        statusCode: 200,
        body: {
          code: 3001,
          msg: '分组名称已存在',
          data: null
        }
      }).as('createGroupRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('button').contains('新增分组').click()
      cy.get('input[placeholder*="分组名称"]').type('默认分组')
      cy.get('.el-dialog button').contains('确定').click()
      
      cy.wait('@createGroupRequest')
      cy.get('.el-message--error').should('contain', '分组名称已存在')
    })

    it('删除有短链接的分组应显示错误提示', () => {
      login()
      mockGroupList()
      
      cy.intercept('DELETE', '/api/group/default', {
        statusCode: 200,
        body: {
          code: 3002,
          msg: '分组下存在短链接，无法删除',
          data: null
        }
      }).as('deleteGroupRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-table__body tr').first().find('button').contains('删除').click()
      cy.get('.el-message-box__btns button').contains('确定').click()
      
      cy.wait('@deleteGroupRequest')
      cy.get('.el-message--error').should('contain', '分组下存在短链接')
    })

    it('网络异常应显示错误提示', () => {
      login()
      
      cy.intercept('GET', '/api/group/list', {
        forceNetworkError: true
      }).as('groupListRequest')

      cy.visit('/group')
      cy.wait('@groupListRequest')
      
      cy.get('.el-message--error').should('exist')
    })
  })
})
