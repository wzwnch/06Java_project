/// <reference types="cypress" />
import './commands'

Cypress.on('uncaught:exception', (err) => {
  console.error('Uncaught exception:', err)
  return false
})
