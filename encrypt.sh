#!/usr/bin/env bash

# ==============================
#    配置：和 JNI 端保持一致
# ==============================

# AES-128 密钥字符串 (最多16字节)
KEY_STRING="MyKeyIs16Byte!!"

# 固定IV的十六进制（16字节 => 32个hex）
IV_HEX="1ad96d5133896ead3d4f7fe51033d295"

# ==============================
#    主体逻辑
# ==============================

# 第一个参数应是待加密的文件
PLAINTEXT_FILE="$1"
if [ ! -f "$PLAINTEXT_FILE" ]; then
  echo "用法: $0 <待加密的文件>"
  echo "例如: $0 shader.txt"
  exit 1
fi

# 读取文件内容
PLAINTEXT=$(cat "$PLAINTEXT_FILE")

# 将 Key 字符串转换为十六进制
KEY_HEX=$(echo -n "$KEY_STRING" | xxd -ps | tr -d '\n')

# 执行加密，并收集输出到变量 ENCRYPTED_OUTPUT
ENCRYPTED_OUTPUT=$(echo -n "$PLAINTEXT" | \
openssl enc -aes-128-cbc \
    -K "$KEY_HEX" \
    -iv "$IV_HEX" \
    -nosalt \
    -base64 \
    -A )

# 输出到终端
echo "$ENCRYPTED_OUTPUT"

# 复制到 macOS 系统剪切板（若在 Linux，可改用 xclip 等）
if command -v pbcopy &> /dev/null; then
  # 将加密后的内容写入剪切板
  echo -n "$ENCRYPTED_OUTPUT" | pbcopy
  echo "已将加密结果复制到系统剪切板。"
else
  echo "pbcopy 命令不可用，未能复制到剪切板。"
fi