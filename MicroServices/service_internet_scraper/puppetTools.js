
// settings adopted from https://intoli.com/blog/not-possible-to-block-chrome-headless/


module.exports = {
  downloadPage : async function(page, url,waitFor) {
    await page.goto(url);
    await page.waitFor(waitFor)
    return await page.content();
  },

  configurePageObject : async function (localPage, localUserAgent) {
  await localPage.setUserAgent(localUserAgent);
 
  // hide the webdriver property by 
  await localPage.evaluateOnNewDocument(() => {
    Object.defineProperty(navigator, 'webdriver', {
      get: () => false,
    });
  });

  await localPage.evaluateOnNewDocument(() => {
  // We can mock this in as much depth as we need for the test.
  window.navigator.chrome = {
    runtime: {
    PlatformOs: {
      MAC: 'mac',
      WIN: 'win',
      ANDROID: 'android',
      CROS: 'cros',
      LINUX: 'linux',
      OPENBSD: 'openbsd',
    },
    PlatformArch: {
      ARM: 'arm',
      X86_32: 'x86-32',
      X86_64: 'x86-64',
    },
    PlatformNaclArch: {
      ARM: 'arm',
      X86_32: 'x86-32',
      X86_64: 'x86-64',
    },
    RequestUpdateCheckStatus: {
      THROTTLED: 'throttled',
      NO_UPDATE: 'no_update',
      UPDATE_AVAILABLE: 'update_available',
    },
    OnInstalledReason: {
      INSTALL: 'install',
      UPDATE: 'update',
      CHROME_UPDATE: 'chrome_update',
      SHARED_MODULE_UPDATE: 'shared_module_update',
    },
    OnRestartRequiredReason: {
      APP_UPDATE: 'app_update',
      OS_UPDATE: 'os_update',
      PERIODIC: 'periodic',
    },
  }
  };
  });

  // Pass the Permissions Test.
  await localPage.evaluateOnNewDocument(() => {
    const originalQuery = window.navigator.permissions.query;
    return window.navigator.permissions.query = (parameters) => (
      parameters.name === 'notifications' ?  Promise.resolve({ state: Notification.permission }) : originalQuery(parameters));
  });

  await localPage.evaluateOnNewDocument(() => {
    // Overwrite the `plugins` property to use a custom getter.
    Object.defineProperty(navigator, 'plugins', {
      // This just needs to have `length > 0` for the current test,
      // but we could mock the plugins too if necessary.
      get: () => [1, 2, 3, 4, 5],
    });
  });

  await localPage.evaluateOnNewDocument(() => {
    // Overwrite the `plugins` property to use a custom getter.
    Object.defineProperty(navigator, 'languages', {
      get: () => ['en-US', 'en'],
    });
  });
}

};
