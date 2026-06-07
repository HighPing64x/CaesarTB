"""
HeikeBook Q绑查询工具 (8e数据库)
基于 https://qb.heikebook.com/ API 逆向
"""

import requests
import re
import os
import json
import time


# ============ PushPlus推送函数 ============
def pushplus_notify(token, title, content):
    """通过PushPlus发送通知"""
    if not token:
        print("警告:未配置PushPlus Token，跳过推送")
        return False

    url = "http://www.pushplus.plus/send"
    data = {
        "token": token,
        "title": title,
        "content": content,
        "template": "html"
    }

    try:
        response = requests.post(url, json=data)
        result = response.json()
        if result.get("code") == 200:
            print("信息:PushPlus推送成功")
            return True
        else:
            print(f"错误:PushPlus推送失败，原因:{result.get('msg')}")
            return False
    except Exception as e:
        print(f"错误:发送PushPlus通知时发生异常:{str(e)}")
        return False


# ============ HeikeBook API ============

# 全局共享 session（保持 cookie）以及通用请求头
_session = None
_common_headers = {
    "accept": "application/json, text/plain, */*",
    "accept-language": "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
    "origin": "https://qb.heikebook.com",
    "user-agent": "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36",
    "sec-ch-ua": '"Microsoft Edge";v="149", "Chromium";v="149", "Not)A;Brand";v="24"',
    "sec-ch-ua-mobile": "?1",
    "sec-ch-ua-platform": '"Android"',
    "dnt": "1",
}


def _get_session():
    """获取或创建共享 session"""
    global _session
    if _session is None:
        _session = requests.Session()
        _session.headers.update(_common_headers)
        # 先访问主页，获取必要的 cookie
        try:
            resp = _session.get("https://qb.heikebook.com/", timeout=15)
            print(f"信息:主页访问状态: {resp.status_code}")
        except Exception as e:
            print(f"警告:访问主页失败: {e}")
    return _session


def get_qb_key():
    """
    获取查询所需的 qb_key
    API: GET https://api.heikebook.com/api/v1/qbapp/get
    """
    session = _get_session()
    url = "https://api.heikebook.com/api/v1/qbapp/get"

    try:
        response = session.get(url, timeout=15)
        if response.status_code == 200:
            data = response.json()
            if data.get("code") == 200:
                qb_key = data["data"]["key"]
                return qb_key
            else:
                print(f"错误:获取 qb_key 失败，API返回: {data}")
                return None
        else:
            print(f"错误:获取 qb_key 失败，HTTP状态码: {response.status_code}")
            return None
    except Exception as e:
        print(f"错误:获取 qb_key 异常: {str(e)}")
        return None


def query_qq_bind(qq_number, qb_key):
    """
    查询QQ绑定信息
    API: GET https://api.heikebook.com/api/v1/sgk/qq/qq?qq={QQ}&qb_key={KEY}
    返回: dict 或 None；如果返回401则返回特殊标记
    """
    session = _get_session()
    url = "https://api.heikebook.com/api/v1/sgk/qq/qq"
    params = {
        "qq": qq_number,
        "qb_key": qb_key
    }

    try:
        response = session.get(url, params=params, timeout=15,
                              headers={"referer": "https://qb.heikebook.com/"})
        if response.status_code == 200:
            data = response.json()
            if data.get("code") == 200:
                info = data["data"]
                return {
                    "qq": info.get("qq", qq_number),
                    "phone": info.get("phone", "未知"),
                    "belonging": info.get("belonging", "未知"),
                    "found": True
                }
            elif data.get("code") == 404:
                return {
                    "qq": qq_number,
                    "phone": None,
                    "belonging": None,
                    "found": False,
                    "msg": data.get("msg", "没有相关记录")
                }
            else:
                print(f"错误:查询QQ {qq_number} 时API返回异常: {data}")
                return None
        elif response.status_code == 401:
            try:
                err_data = response.json()
                err_msg = err_data.get("msg", "未授权")
            except Exception:
                err_msg = "未授权"
            print(f"错误:查询QQ {qq_number} 被拒绝: {err_msg}")
            # 返回特殊标记，包含错误信息
            return {"_need_refresh_key": True, "_error_msg": err_msg}
        else:
            print(f"错误:查询QQ {qq_number} 时HTTP错误: {response.status_code}")
            return None
    except Exception as e:
        print(f"错误:查询QQ {qq_number} 时发生异常: {str(e)}")
        return None


def format_result_html(result):
    """将查询结果格式化为HTML表格"""
    if result is None:
        return "<p style='color:red;'>查询失败</p>"

    if result.get("found"):
        return f"""
        <table border='1' style='border-collapse:collapse;width:100%;text-align:center;'>
            <tr style='background-color:#e6f7ff;'><td colspan='2'><b>✅ 查询成功</b></td></tr>
            <tr><td><b>QQ号</b></td><td>{result['qq']}</td></tr>
            <tr><td><b>绑定手机</b></td><td style='color:red;font-size:18px;'><b>{result['phone']}</b></td></tr>
            <tr><td><b>归属地</b></td><td>{result['belonging']}</td></tr>
        </table>
        """
    else:
        return f"""
        <table border='1' style='border-collapse:collapse;width:100%;text-align:center;'>
            <tr style='background-color:#fff2f0;'><td colspan='2'><b>❌ 未查到记录</b></td></tr>
            <tr><td><b>QQ号</b></td><td>{result['qq']}</td></tr>
            <tr><td><b>原因</b></td><td>{result.get('msg', '没有相关记录')}</td></tr>
        </table>
        """


# ============ 主程序 ============
if __name__ == "__main__":
    print("=" * 50)
    print("  HeikeBook Q绑查询工具 (8e数据库)")
    print("  API来源: https://qb.heikebook.com/")
    print("=" * 50)

    # ====== 配置区 ======
    # PushPlus Token (留空则不推送)
    PUSHPLUS_TOKEN = os.environ.get("PUSHPLUS_TOKEN", "")

    # 要查询的QQ号列表
    QQ_LIST = [
        "983040",       # 测试用:已知有记录的QQ
        "1002144",
        "1004358",
        "24476547",
        # 在此添加更多QQ号...
        # "123456789",
        # "987654321",
    ]

    # 查询间隔(秒)，避免请求过快
    QUERY_INTERVAL = 2
    # ===================

    # 第一步:初始化 session
    print("\n[1/3] 正在初始化会话...")
    _get_session()

    # 第二步:批量查询（每次查询前获取新 key）
    print(f"\n[2/3] 开始查询 {len(QQ_LIST)} 个QQ号...")
    print("-" * 50)

    results = []
    found_count = 0
    not_found_count = 0
    error_count = 0

    for i, qq in enumerate(QQ_LIST, 1):
        # 验证QQ号格式
        if not re.match(r"^\d{5,11}$", qq):
            print(f"警告: [{i}/{len(QQ_LIST)}] QQ号 '{qq}' 格式不正确，跳过")
            continue

        # 每次查询前获取新的 qb_key（key 可能有时效/次数限制）
        qb_key = get_qb_key()
        if not qb_key:
            print(f"错误: [{i}/{len(QQ_LIST)}] 获取 qb_key 失败，跳过 QQ: {qq}")
            error_count += 1
            continue

        print(f"信息: [{i}/{len(QQ_LIST)}] 正在查询 QQ: {qq} ...")

        result = query_qq_bind(qq, qb_key)

        # 检查是否是日限额封顶
        if result and result.get("_need_refresh_key"):
            err_msg = result.get("_error_msg", "")
            if "次数限制" in err_msg or "封顶" in err_msg:
                print(f"警告: 今日免费查询次数已用完，停止后续查询")
                # 剩余未查询的计入错误
                error_count += len(QQ_LIST) - i
                break
            # 其他401错误：刷新key重试一次
            print(f"警告: 查询被拒，刷新 key 重试...")
            qb_key = get_qb_key()
            if qb_key:
                result = query_qq_bind(qq, qb_key)

        if result is None or result.get("_need_refresh_key"):
            error_count += 1
            print(f"错误: QQ {qq} 查询失败")
        elif result.get("found"):
            found_count += 1
            print(f"✅ QQ: {qq} | 手机: {result['phone']} | 归属地: {result['belonging']}")
            results.append(result)
        else:
            not_found_count += 1
            print(f"❌ QQ: {qq} | {result.get('msg', '没有相关记录')}")
            results.append(result)

        # 查询间隔（最后一个不需要等）
        if i < len(QQ_LIST):
            time.sleep(QUERY_INTERVAL)

    # ============ 结果汇总 ============
    print("\n" + "=" * 50)
    print("  查询结果汇总")
    print("=" * 50)
    print(f"  总计查询: {len(QQ_LIST)} 个")
    print(f"  ✅ 查到记录: {found_count} 个")
    print(f"  ❌ 无记录: {not_found_count} 个")
    print(f"  ⚠️ 查询失败: {error_count} 个")
    print("=" * 50)

    # 详细信息
    if found_count > 0:
        print("\n📋 查到记录的QQ号详情:")
        print("-" * 40)
        for r in results:
            if r.get("found"):
                print(f"  QQ: {r['qq']} → 手机: {r['phone']} ({r['belonging']})")

    # PushPlus推送
    if PUSHPLUS_TOKEN:
        print("\n信息:正在推送结果到PushPlus...")
        # 构建通知内容
        content_html = f"""
        <h2>HeikeBook Q绑查询结果</h2>
        <p>查询时间: {time.strftime('%Y-%m-%d %H:%M:%S')}</p>
        <p>总计: {len(QQ_LIST)} | ✅{found_count} | ❌{not_found_count} | ⚠️{error_count}</p>
        <hr/>
        """

        if found_count > 0:
            content_html += "<h3>✅ 查询到记录的QQ:</h3>"
            for r in results:
                if r.get("found"):
                    content_html += f"<p>QQ: <b>{r['qq']}</b> → 手机: <b style='color:red;'>{r['phone']}</b> ({r['belonging']})</p>"

        if not_found_count > 0:
            content_html += "<h3>❌ 无记录:</h3>"
            for r in results:
                if not r.get("found"):
                    content_html += f"<p>QQ: {r['qq']} - {r.get('msg', '没有相关记录')}</p>"

        pushplus_notify(PUSHPLUS_TOKEN, "HeikeBook Q绑查询结果", content_html)
    else:
        print("\n警告:未配置PushPlus Token，跳过推送")
        print("提示:设置环境变量 PUSHPLUS_TOKEN 或直接修改脚本中的 PUSHPLUS_TOKEN 变量即可启用推送")
