#!/usr/bin/env bash

# ==============================
#    配置：与 JNI 端保持一致
# ==============================

# 1. 你的 AES-128 密钥字符串 (最多16字节)。
#    若不足16字节则JNI补0，若超16字节应截断。
KEY_STRING="MyKeyIs16Byte!!"

# 2. 固定IV的十六进制（16字节 => 32个Hex），与 JNI 中 iv 数组一致
IV_HEX="1ad96d5133896ead3d4f7fe51033d295"

# ==============================
#    主体逻辑
# ==============================

# 用法检查
if [ $# -lt 2 ]; then
  echo "用法: $0 <输入文件夹> <输出文件>"
  echo "示例: $0 ./shaders ./encrypt_result.txt"
  exit 1
fi

INPUT_DIR="$1"
OUTPUT_FILE="$2"

# 检查输入文件夹是否存在
if [ ! -d "$INPUT_DIR" ]; then
  echo "错误: $INPUT_DIR 不是有效的文件夹"
  exit 1
fi

# 如果需要，每次执行时清空输出文件
> "$OUTPUT_FILE"

# 先把 Key 字符串转成 Hex
KEY_HEX=$(echo -n "$KEY_STRING" | xxd -ps | tr -d '\n')

# 遍历文件夹中的所有文件
for filepath in "$INPUT_DIR"/*; do
  # 如果是子目录，直接跳过
  if [ -d "$filepath" ]; then
    continue
  fi

  # 取文件名（不含路径）
  filename=$(basename "$filepath")

  # 去掉扩展名
  filename_no_ext="${filename%%.*}"

  # 转大写
  uppercase_name=$(echo "$filename_no_ext" | tr '[:lower:]' '[:upper:]')

  # 读取文件内容到变量
  PLAINTEXT=$(cat "$filepath")

  # 使用 OpenSSL enc 进行 AES-128-CBC 加密 (-nosalt, base64, 无换行)
  ENCRYPTED_OUTPUT=$(
    echo -n "$PLAINTEXT" | \
    openssl enc -aes-128-cbc \
        -K "$KEY_HEX" \
        -iv "$IV_HEX" \
        -nosalt \
        -base64 \
        -A
  )

  # 将结果以指定格式写入输出文件
  # 注意末尾加上分号，符合 C/C++ 常见写法
  echo -e "static const char* $uppercase_name = \"$ENCRYPTED_OUTPUT\";" >> "$OUTPUT_FILE"
  # 添加换行
  echo "" >> "$OUTPUT_FILE"
done

echo "批量加密完成，结果已写入: $OUTPUT_FILE"