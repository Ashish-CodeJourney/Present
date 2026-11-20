FROM nginx:1.27-alpine
COPY deploy/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY docs/ /usr/share/nginx/html/
EXPOSE 8080
