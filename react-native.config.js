module.exports = {
  dependency: {
    platforms: {
      android: {
        packageImportPath: 'import com.zxingscanner.ScannerPackage;',
        packageInstance: 'new ScannerPackage()',
      },
      ios: {},
    },
  },
};
