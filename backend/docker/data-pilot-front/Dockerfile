FROM node:22 AS builder
WORKDIR /build
#COPY . .
COPY ./data-pilot-front .
ENV NODE_OPTIONS="--max-old-space-size=8182"
#RUN npm config set registry https://registry.npmmirror.com
#RUN npm install
#RUN npm run build

FROM nginx:1.27.4
COPY --from=builder /build/dist /usr/share/nginx/html
EXPOSE 80
ENTRYPOINT ["nginx", "-g", "daemon off;"]
