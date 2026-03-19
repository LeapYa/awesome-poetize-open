#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 作者: LeapYa
# 创建日期: 2025-07-15
# 版本: 2.0
# 描述: 中文字体子集化工具，将字体切分为base、level1、level2、other四个子集，生成 font.css（@font-face + unicode-range）和兼容用 unicode_ranges.json

# 使用方法: 
# 1.将font.ttf（字体文件）放在当前目录下
# 2.执行python font_subset.py
# 3.在当前目录下生成font_chunks目录，里面包含base、level1、level2、other四个子集字体文件、font.css 和 unicode_ranges.json
# 4.将font_chunks目录（里面需包含字体文件和font.css）复制到poetize-web/public/static/assets目录下
# 注意: font.css 为主方案（@font-face + unicode-range），unicode_ranges.json 作为兼容回退保留

import os
import subprocess
import json
import sys
from fontTools.ttLib import TTFont
from collections import defaultdict

# 定义输出目录
OUTPUT_DIR = 'font_chunks'
if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

# 读取一级和二级常用字
def read_charset(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        # 过滤掉非汉字字符和空行
        chars = []
        for line in f:
            for char in line.strip():
                if '\u4e00' <= char <= '\u9fff':  # 只保留汉字范围的字符
                    chars.append(char)
        return chars

# 获取字符的unicode编码
def get_unicode(char):
    return f'U+{ord(char):04X}'

# 将unicode范围转换为JS/CSS可用的格式
def unicode_ranges_to_js_css(ranges):
    result = []
    for start, end in ranges:
        if start == end:
            result.append(f'U+{start:04X}')
        else:
            result.append(f'U+{start:04X}-{end:04X}')
    return result

# 合并相邻的unicode范围
def merge_ranges(codes):
    if not codes:
        return []
    
    # 先排序
    sorted_codes = sorted(codes)
    
    # 初始化范围
    ranges = []
    current_range = [sorted_codes[0], sorted_codes[0]]
    
    # 合并相邻范围
    for code in sorted_codes[1:]:
        if code == current_range[1] + 1:
            current_range[1] = code
        else:
            ranges.append(tuple(current_range))
            current_range = [code, code]
    
    # 添加最后一个范围
    ranges.append(tuple(current_range))
    return ranges

# 主函数
def main():
    # 检查字体文件是否存在
    font_file = 'font.ttf'
    if not os.path.exists(font_file):
        print(f"错误：找不到字体文件 {font_file}")
        sys.exit(1)
    
    # 检查输入文件
    level1_file = 'level-1.txt'
    level2_file = 'level-2.txt'
    
    if not os.path.exists(level1_file) or not os.path.exists(level2_file):
        print(f"错误：找不到输入文件 {level1_file} 或 {level2_file}")
        sys.exit(1)
    
    # 读取字符集
    level1_chars = read_charset(level1_file)
    level2_chars = read_charset(level2_file)
    
    print(f"从文件读取的一级汉字数量: {len(level1_chars)} 个字符")
    print(f"从文件读取的二级汉字数量: {len(level2_chars)} 个字符")
    
    # 从原始字体中提取所有字符
    try:
        font = TTFont(font_file)
        all_chars = set()
        for cmap in font['cmap'].tables:
            for code, name in cmap.cmap.items():
                if code >= 32:  # 从ASCII空格开始
                    try:
                        char = chr(code)
                        all_chars.add(char)
                    except Exception:
                        pass
    except Exception as e:
        print(f"错误：无法解析字体文件：{e}")
        sys.exit(1)
    
    print(f"原始字体中的字符总数: {len(all_chars)} 个字符")
    
    # 创建基础字符集（ASCII可打印字符）
    base_chars = [chr(i) for i in range(32, 127)]  # ASCII 32-126
    
    # 添加常用中文标点符号和全角符号
    chinese_punctuations = [
        "·",'—','‘','’','“','”','…','。','《','》','【','】','！','，','？','～'
    ]
    
    # 添加到基础字符集
    for char in chinese_punctuations:
        if char in all_chars and char not in base_chars:
            base_chars.append(char)
    
    # 确保字符唯一性并且只保留字体中存在的字符
    level1_filtered = []
    for char in level1_chars:
        if char in all_chars and char not in base_chars and char not in level1_filtered:
            level1_filtered.append(char)
    
    level2_filtered = []
    for char in level2_chars:
        if char in all_chars and char not in base_chars and char not in level1_filtered and char not in level2_filtered:
            level2_filtered.append(char)
    
    # 获取其他字符（原始字体中但不在以上集合中的字符）
    other_chars = []
    for char in all_chars:
        if char not in base_chars and char not in level1_filtered and char not in level2_filtered:
            other_chars.append(char)
    
    # 统计信息
    print(f"基础字符集(包含中文符号): {len(base_chars)} 个字符")
    print(f"过滤后的一级汉字: {len(level1_filtered)} 个字符")
    print(f"过滤后的二级汉字: {len(level2_filtered)} 个字符")
    print(f"其他字符: {len(other_chars)} 个字符")
    
    # 保存字符集到文件（已注释）
    """
    def save_chars_to_file(chars, filename):
        with open(os.path.join(OUTPUT_DIR, filename), 'w', encoding='utf-8') as f:
            f.write(''.join(chars))
    
    save_chars_to_file(base_chars, 'base_chars.txt')
    save_chars_to_file(level1_filtered, 'level1_chars.txt')
    save_chars_to_file(level2_filtered, 'level2_chars.txt')
    save_chars_to_file(other_chars, 'other_chars.txt')
    """
    
    # 使用pyftsubset生成子集字体（改为woff2格式）
    def create_subset(chars, output_name):
        unicodes = ','.join([get_unicode(char) for char in chars])
        output_path = os.path.join(OUTPUT_DIR, f"font.{output_name}.woff2")
        cmd = [
            'pyftsubset',
            font_file,
            f'--unicodes={unicodes}',
            f'--output-file={output_path}',
            '--layout-features=*',
            '--flavor=woff2',
            "--no-hinting"
        ]
        print(f"正在生成 font.{output_name}.woff2...")
        subprocess.run(cmd)
        return output_path
    
    # 生成子集字体
    base_font = create_subset(base_chars, 'base')
    level1_font = create_subset(level1_filtered, 'level1')
    level2_font = create_subset(level2_filtered, 'level2')
    other_font = create_subset(other_chars, 'other')
    
    # 获取各子集的unicode范围
    def get_ranges(chars):
        codes = [ord(char) for char in chars]
        return merge_ranges(codes)
    
    base_ranges = get_ranges(base_chars)
    level1_ranges = get_ranges(level1_filtered)
    level2_ranges = get_ranges(level2_filtered)
    other_ranges = get_ranges(other_chars)
    
    # 生成unicode_ranges
    ranges = {
        'base': unicode_ranges_to_js_css(base_ranges),
        'level1': unicode_ranges_to_js_css(level1_ranges),
        'level2': unicode_ranges_to_js_css(level2_ranges),
        'other': unicode_ranges_to_js_css(other_ranges)
    }
    
    # 保存为JSON（兼容回退方案）
    with open(os.path.join(OUTPUT_DIR, 'unicode_ranges.json'), 'w', encoding='utf-8') as f:
        json.dump(ranges, f, ensure_ascii=False, indent=2)

    # 生成 font.css（主方案：@font-face + unicode-range，与 cn-font-split 输出格式一致）
    css_chunks = [
        ('base',   'font.base.woff2'),
        ('level1', 'font.level1.woff2'),
        ('level2', 'font.level2.woff2'),
        ('other',  'font.other.woff2'),
    ]
    css_lines = []
    for key, filename in css_chunks:
        css_lines += [
            '@font-face {',
            '  font-family: "MyAwesomeFont";',
            f'  src: url("{filename}") format("woff2");',
            '  font-display: swap;',
            '  font-weight: normal;',
            '  font-style: normal;',
            f'  unicode-range: {", ".join(ranges[key])};',
            '}',
            '',
        ]
    with open(os.path.join(OUTPUT_DIR, 'font.css'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(css_lines))

    print("所有文件已生成完毕！")

if __name__ == '__main__':
    main() 