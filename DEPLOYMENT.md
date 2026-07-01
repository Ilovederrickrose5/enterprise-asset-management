# 窗口1: 启动Nacos
cd D:\Users\30776\Downloads\nacos-server-3.2.2\nacos\bin
.\startup.cmd -m standalone

# 窗口2: 启动认证服务
cd D:\Users\30776\IdeaProjects\enterprise-asset-management
mvn -pl asset-auth spring-boot:run

# 窗口3: 启动业务服务
cd D:\Users\30776\IdeaProjects\enterprise-asset-management
mvn -pl asset-business spring-boot:run

# 窗口4: 启动前端
cd D:\Users\30776\IdeaProjects\enterprise-asset-management\frontend
npm run dev