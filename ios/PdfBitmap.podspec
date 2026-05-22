require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'PdfBitmap'
  s.version        = package['version']
  s.summary        = package['description']
  s.homepage       = 'https://github.com/krymskyimaksym/react-native-pdf-bitmap'
  s.license        = package['license']
  s.author         = package['author']
  s.source         = { git: '' }
  s.platforms      = { ios: '15.1' }
  s.swift_version  = '5.4'

  s.source_files   = '**/*.swift'

  s.dependency 'ExpoModulesCore'
end