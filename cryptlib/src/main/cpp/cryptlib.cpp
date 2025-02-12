#include <jni.h>
#include <string>
#include <openssl/aes.h>
#include <android/log.h>
#include "encrypt_text.h"
#include "openssl/evp.h"
// 统一的 AES 密钥字符串
#define AES_KEY_STRING "MyKeyIs16Byte!!"

#define LOG_TAG "sqsong"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// AES-128 要求密钥长度为 16 字节
static const int AES_KEY_LEN = 16;

// 固定 IV（初始化向量），这里使用全 0 的 16 字节（仅供示例，实际请使用随机IV）
static const unsigned char iv[16] = {
        0x1A, 0xD9, 0x6D, 0x51,
        0x33, 0x89, 0x6E, 0xAD,
        0x3D, 0x4F, 0x7F, 0xE5,
        0x10, 0x33, 0xD2, 0x95
};

/**
 * 将 Base64 编码的字符串转换为二进制数据
 *
 * @param base64_str  Base64 编码字符串
 * @param outBytes    输出的二进制数据缓冲区（由函数内部分配，调用者负责释放）
 * @param outLen      输出数据的实际字节长度
 * @return true 转换成功；false 转换失败
 */
static bool base64ToBytes(const char *base64_str, unsigned char **outBytes, int *outLen) {
    if (!base64_str) return false;
    int base64_len = strlen(base64_str);
    // 分配足够的内存：Base64编码的长度每4个字符对应 3 个字节
    int alloc_len = (base64_len / 4) * 3;
    auto *buffer = new unsigned char[alloc_len];
    // EVP_DecodeBlock 要求输入为 unsigned char*
    int decoded_len = EVP_DecodeBlock(buffer, (const unsigned char *) base64_str, base64_len);
    if (decoded_len < 0) {
        delete[] buffer;
        return false;
    }
    // 根据输入末尾的 '=' 数量调整实际输出长度
    int pad = 0;
    if (base64_len > 0 && base64_str[base64_len - 1] == '=') pad++;
    if (base64_len > 1 && base64_str[base64_len - 2] == '=') pad++;
    decoded_len -= pad;

    *outBytes = buffer;
    *outLen = decoded_len;
    return true;
}

/**
 * bytesToBase64
 * 使用 EVP_EncodeBlock 将二进制数据一次性转为 Base64 编码字符串（以 '\0' 结尾）。
 * @param data       [in]  二进制数据指针
 * @param data_len   [in]  data 的字节数
 * @param outBase64  [out] 输出的Base64字符串指针地址；成功后由调用者负责释放
 * @return bool      成功返回true，失败返回false
 */
bool bytesToBase64(const unsigned char *data, int data_len, char **outBase64) {
    if (!data || data_len <= 0 || !outBase64) {
        return false;
    }

    // Base64 编码后的长度上限约为 4 * ceil(data_len / 3)
    // 对于 EVP_EncodeBlock，通常计算：4 * ((data_len + 2) / 3)
    int encode_len = 4 * ((data_len + 2) / 3);
    // +1 用于字符串结束符 '\0'
    auto *encode_buf = new unsigned char[encode_len + 1];
    // EVP_EncodeBlock 一次性完成编码
    // 返回值是实际写入 encode_buf 的 Base64 字符数量（不含 '\0'）
    int out_len = EVP_EncodeBlock(encode_buf, data, data_len);
    if (out_len < 0) {
        delete[] encode_buf;
        return false;
    }
    // 末尾补 '\0' 形成 C 字符串
    encode_buf[out_len] = '\0';
    // 将结果指针返回给调用者
    *outBase64 = reinterpret_cast<char *>(encode_buf);
    return true;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sqsong_cryptlib_CryptLib_getDecryptedString(JNIEnv *env, jobject thiz, jint key) {
    // 根据 key 类型选择对应的 Base64 加密字符串
    const char *encryptedBase64;
    if (key == 1) {
        encryptedBase64 = SHADER_BRIGHTNESS_FRAG;
    } else if (key == 2) {
        encryptedBase64 = SHADER_FRAG_CLARITY_FILTER;
    } else if (key == 3) {
        encryptedBase64 = SHADER_FRAG_CONTRAST_FILTER;
    } else {
        LOGE("getDecryptedString: Invalid key = %d", key);
        return nullptr;  // 无效的 key 类型
    }

    // 统一使用 AES_KEY_STRING 生成 AES-128 密钥（不足 16 字节则右侧补 0）
    unsigned char aes_key[AES_KEY_LEN];
    memset(aes_key, 0, AES_KEY_LEN);

    size_t keyStrLen = strlen(AES_KEY_STRING);
    if (keyStrLen > AES_KEY_LEN) {
        LOGE("getDecryptedString: AES_KEY_STRING length %zu is bigger than AES_KEY_LEN %d.",
             keyStrLen, AES_KEY_LEN);
        return nullptr;
    }
    memcpy(aes_key, AES_KEY_STRING, keyStrLen);

    // 1. Base64 解码：将加密后的字符串转换为二进制数据
    unsigned char *encryptedData = nullptr;
    int encryptedDataLen = 0;
    if (!base64ToBytes(encryptedBase64, &encryptedData, &encryptedDataLen)) {
        LOGE("getDecryptedString: base64ToBytes failed. (Base64 string = %s)", encryptedBase64);
        return nullptr;
    }

    // 2. 使用 OpenSSL EVP 接口进行 AES-128-CBC 解密
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) {
        LOGE("getDecryptedString: Failed to create EVP_CIPHER_CTX.");
        delete[] encryptedData;
        return nullptr;
    }

    // 注意这里使用固定IV
    if (EVP_DecryptInit_ex(ctx, EVP_aes_128_cbc(), nullptr, aes_key, iv) != 1) {
        LOGE("getDecryptedString: EVP_DecryptInit_ex failed. Possibly wrong cipher or IV.");
        EVP_CIPHER_CTX_free(ctx);
        delete[] encryptedData;
        return nullptr;
    }

    // 分配足够的缓冲区：密文长度 + 一个分组大小
    int block_size = EVP_CIPHER_CTX_block_size(ctx);
    int out_buf_len = encryptedDataLen + block_size;
    auto *out_buf = new unsigned char[out_buf_len];

    int out_len1 = 0;
    if (EVP_DecryptUpdate(ctx, out_buf, &out_len1, encryptedData, encryptedDataLen) != 1) {
        LOGE("getDecryptedString: EVP_DecryptUpdate failed.");
        EVP_CIPHER_CTX_free(ctx);
        delete[] encryptedData;
        delete[] out_buf;
        return nullptr;
    }

    int out_len2 = 0;
    if (EVP_DecryptFinal_ex(ctx, out_buf + out_len1, &out_len2) != 1) {
        LOGE("getDecryptedString: EVP_DecryptFinal_ex failed. Possibly wrong key or corrupt data.");
        EVP_CIPHER_CTX_free(ctx);
        delete[] encryptedData;
        delete[] out_buf;
        return nullptr;
    }

    int total_out_len = out_len1 + out_len2;
    EVP_CIPHER_CTX_free(ctx);
    delete[] encryptedData;

    // 确保解密后的数据以 '\0' 结尾（假定为字符串）
    if (total_out_len < out_buf_len) {
        out_buf[total_out_len] = '\0';
    } else {
        // 理论上不会出现这种情况，因为 out_buf_len = encryptedDataLen + block_size
        out_buf[out_buf_len - 1] = '\0';
    }

    jstring result = env->NewStringUTF(reinterpret_cast<const char *>(out_buf));
    if (!result) {
        LOGE("getDecryptedString: NewStringUTF returned null. Possibly out of memory.");
    }

    delete[] out_buf;
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sqsong_cryptlib_CryptLib_encryptString(JNIEnv *env, jobject thiz, jstring text) {
// 1) 检查输入文本是否有效
    if (!text) {
        LOGE("encryptString: input text is null");
        return nullptr;
    }

    // 将 jstring 转成 C 字符串 (Modified UTF-8)
    const char *plainText = env->GetStringUTFChars(text, nullptr);
    if (!plainText) {
        LOGE("encryptString: failed to get UTF chars from jstring");
        return nullptr;
    }

    // 2) 准备 AES-128-CBC 的 key
    unsigned char aes_key[AES_KEY_LEN];
    memset(aes_key, 0, AES_KEY_LEN);

    size_t keyStrLen = strlen(AES_KEY_STRING);
    if (keyStrLen > AES_KEY_LEN) {
        LOGE("encryptString: AES_KEY_STRING length %zu > AES_KEY_LEN %d", keyStrLen, AES_KEY_LEN);
        env->ReleaseStringUTFChars(text, plainText);
        return nullptr;
    }
    // 拷贝到固定长度的 aes_key 数组中
    memcpy(aes_key, AES_KEY_STRING, keyStrLen);

    // 3) 初始化加密上下文
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) {
        LOGE("encryptString: failed to create EVP_CIPHER_CTX");
        env->ReleaseStringUTFChars(text, plainText);
        return nullptr;
    }

    // 注意，这里的 iv 要和 getDecryptedString 中一致
    // （如果在代码里有声明 static const unsigned char iv[16], 可以直接用）
    if (EVP_EncryptInit_ex(ctx, EVP_aes_128_cbc(), nullptr, aes_key, iv) != 1) {
        LOGE("encryptString: EVP_EncryptInit_ex failed");
        EVP_CIPHER_CTX_free(ctx);
        env->ReleaseStringUTFChars(text, plainText);
        return nullptr;
    }

    // 4) 执行加密
    // 分配输出缓冲：原文长度 + 一个分组大小，足以容纳加密后数据
    int plain_len = static_cast<int>(strlen(plainText));
    int block_size = EVP_CIPHER_CTX_block_size(ctx);
    int out_buf_len = plain_len + block_size;

    auto *out_buf = new unsigned char[out_buf_len];

    // 加密过程
    int out_len1 = 0;
    if (EVP_EncryptUpdate(ctx, out_buf, &out_len1,
                          reinterpret_cast<const unsigned char *>(plainText), plain_len) != 1) {
        LOGE("encryptString: EVP_EncryptUpdate failed");
        EVP_CIPHER_CTX_free(ctx);
        delete[] out_buf;
        env->ReleaseStringUTFChars(text, plainText);
        return nullptr;
    }

    int out_len2 = 0;
    if (EVP_EncryptFinal_ex(ctx, out_buf + out_len1, &out_len2) != 1) {
        LOGE("encryptString: EVP_EncryptFinal_ex failed. Possibly wrong key or IV?");
        EVP_CIPHER_CTX_free(ctx);
        delete[] out_buf;
        env->ReleaseStringUTFChars(text, plainText);
        return nullptr;
    }

    // total cipher length
    int cipher_len = out_len1 + out_len2;

    // 清理上下文
    EVP_CIPHER_CTX_free(ctx);

    // 5) 对加密结果进行 Base64 编码
    char *base64Enc = nullptr;
    if (!bytesToBase64(out_buf, cipher_len, &base64Enc)) {
        LOGE("encryptString: bytesToBase64 failed");
        delete[] out_buf;
        env->ReleaseStringUTFChars(text, plainText);
        return nullptr;
    }

    // out_buf 不再需要
    delete[] out_buf;

    // 释放原文字符串
    env->ReleaseStringUTFChars(text, plainText);

    // 6) 将Base64编码后的字符串返回给Java层
    jstring result = env->NewStringUTF(base64Enc);
    if (!result) {
        LOGE("encryptString: NewStringUTF returned null. Possibly out of memory");
    }

    // 释放base64Enc
    free(base64Enc);

    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sqsong_cryptlib_CryptLib_decryptedString(JNIEnv *env, jobject thiz, jstring text) {
// 1. 检查输入
    if (text == nullptr) {
        LOGE("decryptedString: input text is null");
        return nullptr;
    }

    // 将 jstring 转成 C 风格字符串（Modified UTF-8）
    const char *base64Cipher = env->GetStringUTFChars(text, nullptr);
    if (!base64Cipher) {
        LOGE("decryptedString: failed to convert jstring to UTF-8");
        return nullptr;
    }

    // 2. Base64 解码
    unsigned char *cipherData = nullptr;
    int cipherDataLen = 0;
    if (!base64ToBytes(base64Cipher, &cipherData, &cipherDataLen)) {
        LOGE("decryptedString: base64ToBytes failed. (Base64 string = %s)", base64Cipher);
        env->ReleaseStringUTFChars(text, base64Cipher);
        return nullptr;
    }

    // 释放Java层的Base64字符串
    env->ReleaseStringUTFChars(text, base64Cipher);

    // 3. 准备AES密钥
    unsigned char aes_key[AES_KEY_LEN];
    memset(aes_key, 0, AES_KEY_LEN);

    size_t keyStrLen = strlen(AES_KEY_STRING);
    if (keyStrLen > AES_KEY_LEN) {
        LOGE("decryptedString: AES_KEY_STRING length %zu > %d", keyStrLen, AES_KEY_LEN);
        delete[] cipherData;
        return nullptr;
    }
    memcpy(aes_key, AES_KEY_STRING, keyStrLen);

    // 4. 创建解密上下文
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) {
        LOGE("decryptedString: failed to create EVP_CIPHER_CTX");
        delete[] cipherData;
        return nullptr;
    }

    // 初始化解密 (AES-128-CBC + PKCS7 padding)
    if (EVP_DecryptInit_ex(ctx, EVP_aes_128_cbc(), nullptr, aes_key, iv) != 1) {
        LOGE("decryptedString: EVP_DecryptInit_ex failed");
        EVP_CIPHER_CTX_free(ctx);
        delete[] cipherData;
        return nullptr;
    }

    // 5. 分配缓冲区 = cipherDataLen + 一个分组大小
    int blockSize = EVP_CIPHER_CTX_block_size(ctx);
    int outBufLen = cipherDataLen + blockSize;
    auto *outBuf = new unsigned char[outBufLen];

    // 6. 解密数据
    int outLen1 = 0;
    if (EVP_DecryptUpdate(ctx, outBuf, &outLen1, cipherData, cipherDataLen) != 1) {
        LOGE("decryptedString: EVP_DecryptUpdate failed");
        EVP_CIPHER_CTX_free(ctx);
        delete[] cipherData;
        delete[] outBuf;
        return nullptr;
    }

    int outLen2 = 0;
    if (EVP_DecryptFinal_ex(ctx, outBuf + outLen1, &outLen2) != 1) {
        LOGE("decryptedString: EVP_DecryptFinal_ex failed. Possibly wrong key/IV or corrupt data.");
        EVP_CIPHER_CTX_free(ctx);
        delete[] cipherData;
        delete[] outBuf;
        return nullptr;
    }

    int totalPlainLen = outLen1 + outLen2;

    // 清理上下文&释放cipherData
    EVP_CIPHER_CTX_free(ctx);
    delete[] cipherData;

    // 7. 补'\0'以构造C字符串（假设明文是UTF-8可打印文本）
    if (totalPlainLen < outBufLen) {
        outBuf[totalPlainLen] = '\0';
    } else {
        // 理论上不会出现这种情况
        outBuf[outBufLen - 1] = '\0';
    }

    // 8. 转成jstring返回给Java层
    jstring resultStr = env->NewStringUTF(reinterpret_cast<const char *>(outBuf));
    if (!resultStr) {
        LOGE("decryptedString: NewStringUTF returned null. Possibly invalid UTF-8 or out of memory");
    }

    delete[] outBuf;
    return resultStr;
}