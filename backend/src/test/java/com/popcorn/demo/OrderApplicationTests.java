package com.popcorn.demo;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.popcorn.demo.domain.order")
class OrderApplicationTests {
}
