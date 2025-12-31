package com.popcorn.demo;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.popcorn.demo")
@ExcludeClassNamePatterns(".*DemoApplicationTests")
class DemoApplicationTests {
}
