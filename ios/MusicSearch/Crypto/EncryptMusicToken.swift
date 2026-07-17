import Foundation

/// 酷我音乐请求接口参数加密（对应 Android EncryptMusicToken）
/// 一套类 DES 分组加密算法，密钥 "ylzsxkwm"，输出经 Base64。
/// 使用 Int64 精确复刻 Java long 的位运算行为。
enum EncryptMusicToken {

    private static let first: [UInt8] = Array("ylzsxkwm".utf8)

    // 2 的幂次表（0..63），最后一项对应 Java Long.MIN_VALUE（符号位）
    private static let d: [Int64] = {
        var arr: [Int64] = []
        for i in 0..<63 { arr.append(Int64(1) << i) }
        arr.append(Int64.min) // 2^63 的位模式
        return arr
    }()

    // 初始置换表 IP
    private static let e_IP: [Int] = [
        57, 49, 41, 33, 25, 17, 9, 1, 59, 51, 43, 35, 27, 19, 11, 3,
        61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31, 23, 15, 7,
        56, 48, 40, 32, 24, 16, 8, 0, 58, 50, 42, 34, 26, 18, 10, 2,
        60, 52, 44, 36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22, 14, 6
    ]

    // 扩展置换表 E（-1 项跳过）
    private static let e_E: [Int] = [
        31, 0, 1, 2, 3, 4, -1, -1, 3, 4, 5, 6, 7, 8, -1, -1,
        7, 8, 9, 10, 11, 12, -1, -1, 11, 12, 13, 14, 15, 16, -1, -1,
        15, 16, 17, 18, 19, 20, -1, -1, 19, 20, 21, 22, 23, 24, -1, -1,
        23, 24, 25, 26, 27, 28, -1, -1, 27, 28, 29, 30, 31, 30, -1, -1
    ]

    // 8 个 S 盒（每个 64 项），已将 Java char 字面量转为整数值
    private static let sboxes: [[UInt8]] = [
        [14, 4, 3, 15, 2, 13, 5, 3, 13, 14, 6, 9, 11, 2, 0, 5, 4, 1, 10, 12, 15, 6, 9, 10, 1, 8, 12, 7, 8, 11, 7, 0, 0, 15, 10, 5, 14, 4, 9, 10, 7, 8, 12, 3, 13, 1, 3, 6, 15, 12, 6, 11, 2, 9, 5, 0, 4, 2, 11, 14, 1, 7, 8, 13],
        [15, 0, 9, 5, 6, 10, 12, 9, 8, 7, 2, 12, 3, 13, 5, 2, 1, 14, 7, 8, 11, 4, 0, 3, 14, 11, 13, 6, 4, 1, 10, 15, 3, 13, 12, 11, 15, 3, 6, 0, 4, 10, 1, 7, 8, 4, 11, 14, 13, 8, 0, 6, 2, 15, 9, 5, 7, 1, 10, 12, 14, 2, 5, 9],
        [10, 13, 1, 11, 6, 8, 11, 5, 9, 4, 12, 2, 15, 3, 2, 14, 0, 6, 13, 1, 3, 15, 4, 10, 14, 9, 7, 12, 5, 0, 8, 7, 13, 1, 2, 4, 3, 6, 12, 11, 0, 13, 5, 14, 6, 8, 15, 2, 7, 10, 8, 15, 4, 9, 11, 5, 9, 0, 14, 3, 10, 7, 1, 12],
        [7, 10, 1, 15, 0, 12, 11, 5, 14, 9, 8, 3, 9, 7, 4, 8, 13, 6, 2, 1, 6, 11, 12, 2, 3, 0, 5, 14, 10, 13, 15, 4, 13, 3, 4, 9, 6, 10, 1, 12, 11, 0, 2, 5, 0, 13, 14, 2, 8, 15, 7, 4, 15, 1, 10, 7, 5, 6, 12, 11, 3, 8, 9, 14],
        [2, 4, 8, 15, 7, 10, 13, 6, 4, 1, 3, 12, 11, 7, 14, 0, 12, 2, 5, 9, 10, 13, 0, 3, 1, 11, 15, 5, 6, 8, 9, 14, 14, 11, 5, 6, 4, 1, 3, 10, 2, 12, 15, 0, 13, 2, 8, 5, 11, 8, 0, 15, 7, 14, 9, 4, 12, 7, 10, 9, 1, 13, 6, 3],
        [12, 9, 0, 7, 9, 2, 14, 1, 10, 15, 3, 4, 6, 12, 5, 11, 1, 14, 13, 0, 2, 8, 7, 13, 15, 5, 4, 10, 8, 3, 11, 6, 10, 4, 6, 11, 7, 9, 0, 6, 4, 2, 13, 1, 9, 15, 3, 8, 15, 3, 1, 14, 12, 5, 11, 0, 2, 12, 14, 7, 5, 10, 8, 13],
        [4, 1, 3, 10, 15, 12, 5, 0, 2, 11, 9, 6, 8, 7, 6, 9, 11, 4, 12, 15, 0, 3, 10, 5, 14, 13, 7, 8, 13, 14, 1, 2, 13, 6, 14, 9, 4, 1, 2, 14, 11, 13, 5, 0, 1, 10, 8, 3, 0, 11, 3, 5, 9, 4, 15, 2, 7, 8, 12, 15, 10, 7, 6, 12],
        [13, 7, 10, 0, 6, 9, 5, 15, 8, 4, 3, 10, 11, 14, 12, 5, 2, 11, 9, 6, 15, 12, 0, 3, 4, 1, 14, 13, 1, 2, 7, 8, 1, 2, 12, 15, 10, 4, 0, 3, 13, 14, 6, 9, 7, 8, 9, 6, 15, 1, 5, 12, 3, 10, 14, 5, 8, 7, 11, 0, 4, 13, 2, 11]
    ]

    // 置换表 P
    private static let e_P: [Int] = [
        15, 6, 19, 20, 28, 11, 27, 16, 0, 14, 22, 25, 4, 17, 30, 9,
        1, 7, 23, 13, 31, 26, 2, 8, 18, 12, 29, 5, 21, 10, 3, 24
    ]

    // 逆置换表 IP^-1
    private static let e_IPinv: [Int] = [
        39, 7, 47, 15, 55, 23, 63, 31, 38, 6, 46, 14, 54, 22, 62, 30,
        37, 5, 45, 13, 53, 21, 61, 29, 36, 4, 44, 12, 52, 20, 60, 28,
        35, 3, 43, 11, 51, 19, 59, 27, 34, 2, 42, 10, 50, 18, 58, 26,
        33, 1, 41, 9, 49, 17, 57, 25, 32, 0, 40, 8, 48, 16, 56, 24
    ]

    // PC-1 选位表
    private static let pc1: [Int] = [
        56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1,
        58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 62, 54, 46, 38,
        30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 60, 52, 44, 36,
        28, 20, 12, 4, 27, 19, 11, 3
    ]

    // PC-2 选位表（-1 跳过）
    private static let pc2: [Int] = [
        13, 16, 10, 23, 0, 4, -1, -1, 2, 27, 14, 5, 20, 9, -1, -1,
        22, 18, 11, 3, 25, 7, -1, -1, 15, 6, 26, 19, 12, 1, -1, -1,
        40, 51, 30, 36, 46, 54, -1, -1, 29, 39, 50, 44, 32, 47, -1, -1,
        43, 48, 38, 55, 33, 52, -1, -1, 45, 41, 49, 35, 28, 31, -1, -1
    ]

    // 循环移位表
    private static let shifts: [Int] = [1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1]

    // 掩码辅助：m[1]=0x100001, m[2]=0x300003
    private static let m: [Int64] = [0, 0x100001, 0x300003]

    // 选位置换：从 j2 中按 iArr 表选位重组（i2 为表长度）
    private static func permute(_ iArr: [Int], _ i2: Int, _ j2: Int64) -> Int64 {
        var j3: Int64 = 0
        for i3 in 0..<i2 {
            let v = iArr[i3]
            if v >= 0 && (d[v] & j2) != 0 {
                j3 |= d[v]
            }
        }
        return j3
    }

    // 子密钥生成
    private static func generateSubkeys(_ j2: Int64, _ jArr: inout [Int64], _ i2: Int) {
        var a2 = permute(pc1, 56, j2)
        for i3 in 0..<16 {
            let lm = Int64(shifts[i3])
            a2 = ((a2 & ~m[lm]) >> lm) | ((m[lm] & a2) << (28 - shifts[i3]))
            jArr[i3] = permute(pc2, 64, a2)
        }
        if i2 == 1 {
            for i4 in 0..<8 {
                let tmp = jArr[i4]
                let i5 = 15 - i4
                jArr[i4] = jArr[i5]
                jArr[i5] = tmp
            }
        }
    }

    // 16 轮 Feistel 加密
    private static func encryptBlock(_ jArr: [Int64], _ j2: Int64) -> Int64 {
        var p = permute(e_IP, 64, j2)
        var s0 = Int32(truncatingIfNeeded: p & 0xFFFFFFFF)
        var s1 = Int32(truncatingIfNeeded: (p & ~Int64(0xFFFFFFFF)) >> 32)

        for i2 in 0..<16 {
            var r = Int64(s1)
            r = permute(e_E, 64, r)
            r ^= jArr[i2]
            var t = [UInt8](repeating: 0, count: 8)
            for i3 in 0..<8 {
                t[i3] = UInt8(truncatingIfNeeded: (r >> (i3 * 8)) & 0xFF)
            }
            var u: UInt32 = 0
            for w in stride(from: 7, through: 0, by: -1) {
                u <<= 4
                u |= UInt32(sboxes[w][Int(t[w])])
            }
            r = Int64(u)
            r = permute(e_P, 32, r)
            let q = Int64(s0)
            s0 = s1
            s1 = Int32(truncatingIfNeeded: q ^ r)
        }
        let v = s0
        s0 = s1
        s1 = v
        p = (Int64(s0) & 0xFFFFFFFF) | ((Int64(s1) << 32) & ~Int64(0xFFFFFFFF))
        p = permute(e_IPinv, 64, p)
        return p
    }

    /// 对字节数组分组加密（对应 Java a(byte[], int, byte[])）
    private static func encryptBytes(_ bArr: [UInt8], _ i2: Int, _ bArr2: [UInt8]) -> [UInt8] {
        // 8 字节密钥转 long（小端，符号扩展同 Java）
        var key: Int64 = 0
        for i in 0..<8 {
            let b = Int8(bitPattern: bArr2[i])
            key |= Int64(b) << (i * 8)
        }

        let i5 = i2 / 8
        var subkeys = [Int64](repeating: 0, count: 16)
        generateSubkeys(key, &subkeys, 0)

        var jArr2 = [Int64](repeating: 0, count: i5)
        for i7 in 0..<i5 {
            var block: Int64 = 0
            for i8 in 0..<8 {
                let b = Int8(bitPattern: bArr[i7 * 8 + i8])
                block |= Int64(b) << (i8 * 8)
            }
            jArr2[i7] = block
        }

        var jArr3 = [Int64](repeating: 0, count: i5 + 1)
        for i9 in 0..<i5 {
            jArr3[i9] = encryptBlock(subkeys, jArr2[i9])
        }

        // 尾部不足 8 字节的部分
        let i11 = i5 * 8
        let i12 = i2 - i11
        var j3: Int64 = 0
        if i12 > 0 {
            for i13 in 0..<i12 {
                let b = Int8(bitPattern: bArr[i11 + i13])
                j3 |= Int64(b) << (i13 * 8)
            }
        }
        jArr3[i5] = encryptBlock(subkeys, j3)

        // long 数组转字节数组（小端）
        var result = [UInt8](repeating: 0, count: jArr3.count * 8)
        var idx = 0
        for block in jArr3 {
            for i17 in 0..<8 {
                result[idx] = UInt8(truncatingIfNeeded: (block >> (i17 * 8)) & 0xFF)
                idx += 1
            }
        }
        return result
    }

    /// 对外加密入口（对应 Java encrypt(String)）
    /// 明文 -> DES 加密(密钥 "ylzsxkwm") -> Base64 -> 字符串
    static func encrypt(_ content: String) -> String {
        let bytes = Array(content.utf8)
        let encrypted = encryptBytes(bytes, bytes.count, first)
        return EncryptMusicHandler.encode(encrypted)
    }
}
