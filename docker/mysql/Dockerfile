#FROM mysql:5.7

# arm
FROM ibex/debian-mysql-server-5.7

COPY my.cnf /etc/mysql/my.cnf

WORKDIR /data/mysql
ENV MYSQL_ROOT_PASSWORD=root

EXPOSE 3306

