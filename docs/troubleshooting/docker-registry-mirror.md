# Docker Desktop镜像源无法解析

## 本机实际现象

拉取Docker Hub镜像时出现：

```text
dial tcp: lookup docker.mirrors.ustc.edu.cn: no such host
```

Compose配置和镜像标签本身可能完全正确，但Docker Desktop的全局`registry-mirrors`仍会把请求转发到不可用地址。

## 诊断

```powershell
docker info
docker context inspect
Resolve-DnsName docker.mirrors.ustc.edu.cn
```

区分三类问题：镜像标签不存在、代理DNS失败、下载过程中连接被关闭。不要看到`pull failed`就直接修改项目镜像名。

## 本项目采取的处理

项目文件继续使用官方镜像名。当前机器通过可用代理下载镜像后，在本地标记为官方名称，因此Compose保持可移植性。

## 长期修复

Docker Desktop的镜像源属于机器级设置。修改前应记录原配置，并确认是否由学校、公司网络或安全软件统一管理。删除不可用镜像源或替换为组织批准的镜像仓库后，需要重启Docker Engine并重新验证拉取。

项目不自动修改Docker Desktop全局设置，因为这可能影响本机其他项目和企业网络策略。
