FROM node:24-bookworm-slim
WORKDIR /tool
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY bootstrap.mjs ./
USER node
CMD ["node", "bootstrap.mjs"]
