const { defineConfig } = require("@playwright/test");

module.exports = defineConfig({
  testDir: "./e2e",
  timeout: 60000,
  expect: {
    timeout: 15000,
  },
  reporter: "list",
  use: {
    baseURL: "http://127.0.0.1:3000",
    headless: true,
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
});
