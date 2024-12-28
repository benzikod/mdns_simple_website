# mdns_simple_website
-----
节点发现服务:
在此项目中,可以透过修改 SERVICE_NAME、SERVICE_TYPE、PORT来配置服务；
项目包含方法getLeaderIpv4、start、stop、initializeMDNS；
并附带一个简易的WebHandler并透过类加载方式用于测试服务状况。
-----
此项目代码原先为集群连线初步连线方式,透过获得最优先注册服务机器Ip进行选主(先到先得选主)。
