require "json"
package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-scanner-camera"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = "https://github.com/Nach02/react-native-scanner-camera"
  s.license      = "MIT"
  s.authors      = { "Nach02" => "ignaciomartinez@vaypol.com.ar" }
  s.platforms    = { :ios => "13.4" }
  s.source       = { :git => "https://github.com/Nach02/react-native-scanner-camera.git", :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.dependency   "React-Core"
  s.pod_target_xcconfig = { "SWIFT_VERSION" => "5.0" }
end
