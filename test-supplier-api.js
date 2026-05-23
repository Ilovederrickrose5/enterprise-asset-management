const axios = require('axios');

async function testSupplierAPI() {
  try {
    // 首先获取token
    const loginResponse = await axios.post('http://localhost:8080/api/auth/login', {
      username: 'admin',
      password: '123456'
    });
    
    const token = loginResponse.data.data.token;
    console.log('获取到的token:', token);
    
    // 然后调用供应商API
    const supplierResponse = await axios.get('http://localhost:8080/api/suppliers', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('供应商API响应:', supplierResponse.data);
  } catch (error) {
    console.error('测试失败:', error);
  }
}

testSupplierAPI();