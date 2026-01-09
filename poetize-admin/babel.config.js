module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset'
  ],
  plugins: [
    [
      'component',
      {
        libraryName: 'element-ui-ce',
        styleLibraryName: 'theme-chalk'
      }
    ]
  ]
}
