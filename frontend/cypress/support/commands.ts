/// <reference types="cypress" />

Cypress.Commands.add('login', (username: string, password: string) => {
  cy.visit('/login')
  cy.get('input[placeholder*="用户名"]').clear().type(username)
  cy.get('input[placeholder*="密码"]').clear().type(password)
  cy.get('button').contains('登录').click()
})

Cypress.Commands.add('logout', () => {
  cy.get('.header-user').click()
  cy.contains('退出登录').click()
})

Cypress.Commands.add('register', (username: string, password: string, phone?: string, mail?: string) => {
  cy.visit('/register')
  cy.get('input[placeholder*="用户名"]').clear().type(username)
  cy.get('input[placeholder*="密码"]').eq(0).clear().type(password)
  cy.get('input[placeholder*="确认密码"]').clear().type(password)
  if (phone) {
    cy.get('input[placeholder*="手机号"]').clear().type(phone)
  }
  if (mail) {
    cy.get('input[placeholder*="邮箱"]').clear().type(mail)
  }
  cy.get('button').contains('注册').click()
})

declare global {
  namespace Cypress {
    interface Chainable {
      login(username: string, password: string): Chainable<void>
      logout(): Chainable<void>
      register(username: string, password: string, phone?: string, mail?: string): Chainable<void>
    }
  }
}

export {}
