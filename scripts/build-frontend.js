/**
 * kiftd 前端构建脚本（npm run build）
 *
 * 1) JS：将 webContext/src/js 下的源码模块按顺序拼接后使用 terser 压缩，
 *    输出为 webContext/js/*.min.js（保留 UTF-8 BOM，与历史压缩文件一致）。
 *    - home 页面：由多个功能模块拼接而成（core/view/auth/folder/file/upload/notice/account-admin）。
 *    - 独立页面：login.js / signup.js / kplayer.js 单文件直接压缩。
 * 2) CSS：src/css/overrall.css 使用 clean-css 压缩为 css/overrall.min.css。
 *
 * 设计说明：
 * - JS 采用"拼接"而非 ES Module bundle，以保持全局作用域语义，
 *   保证页面 HTML onclick 等对全局函数/变量的引用不受影响。
 * - 模块加载顺序即拼接顺序，新增模块时在下方 jsTargets 中调整即可。
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { minify } = require('terser');
const CleanCSS = require('clean-css');

const rootDir = path.resolve(__dirname, '..');
const jsSrcDir = path.join(rootDir, 'webContext', 'src', 'js');
const cssSrcFile = path.join(rootDir, 'webContext', 'src', 'css', 'overrall.css');
const jsOutDir = path.join(rootDir, 'webContext', 'js');
const cssOutFile = path.join(rootDir, 'webContext', 'css', 'overrall.min.css');
const BOM = Buffer.from([0xef, 0xbb, 0xbf]);

// JS 构建目标：输出文件名 → 源码模块列表（按拼接顺序，依赖在前）
const jsTargets = [
  {
    name: 'home.min.js',
    modules: ['core.js', 'view.js', 'auth.js', 'folder.js', 'file.js', 'upload.js', 'notice.js', 'account-admin.js']
  },
  { name: 'login.min.js', modules: ['login.js'] },
  { name: 'signup.min.js', modules: ['signup.js'] },
  { name: 'kplayer.min.js', modules: ['kplayer.js'] }
];

async function buildJs(name, modules) {
  const parts = [];
  for (const mod of modules) {
    const file = path.join(jsSrcDir, mod);
    if (!fs.existsSync(file)) {
      console.warn('[build] 跳过缺失模块: ' + mod);
      continue;
    }
    parts.push(fs.readFileSync(file, 'utf8'));
  }
  if (parts.length === 0) {
    console.error('[build] 错误：' + name + ' 未找到任何源码模块');
    process.exit(1);
  }
  const source = parts.join('\n');
  const result = await minify(source, {
    compress: true,
    mangle: true,
    output: { ascii_only: false }
  });
  if (result.error) {
    console.error('[build] ' + name + ' 压缩失败: ' + result.error);
    process.exit(1);
  }
  const outFile = path.join(jsOutDir, name);
  fs.writeFileSync(outFile, Buffer.concat([BOM, Buffer.from(result.code, 'utf8')]));
  return { outFile, sizeKB: (fs.statSync(outFile).size / 1024).toFixed(1), modules: modules.length };
}

function buildCss() {
  if (!fs.existsSync(cssSrcFile)) {
    console.warn('[build] 跳过缺失 CSS 源文件: ' + cssSrcFile);
    return null;
  }
  const source = fs.readFileSync(cssSrcFile, 'utf8');
  const output = new CleanCSS({ level: 2 }).minify(source);
  if (output.errors && output.errors.length > 0) {
    console.error('[build] CSS 压缩失败: ' + output.errors.join('; '));
    process.exit(1);
  }
  fs.writeFileSync(cssOutFile, Buffer.concat([BOM, Buffer.from(output.styles, 'utf8')]));
  return { outFile: cssOutFile, sizeKB: (fs.statSync(cssOutFile).size / 1024).toFixed(1) };
}

/**
 * 为 webContext 下所有 HTML 中引用的自有构建产物（home/login/signup/kplayer.min.js 与 overrall.min.css）
 * 追加基于内容 hash 的版本参数（?v=xxxxxxxx），内容不变则版本不变，内容变更自动失效浏览器缓存。
 */
function stampVersions() {
  const hashMap = {};
  const outs = [
    path.join(jsOutDir, 'home.min.js'),
    path.join(jsOutDir, 'login.min.js'),
    path.join(jsOutDir, 'signup.min.js'),
    path.join(jsOutDir, 'kplayer.min.js'),
    cssOutFile
  ];
  for (const f of outs) {
    if (fs.existsSync(f)) {
      hashMap[path.basename(f)] = crypto.createHash('md5').update(fs.readFileSync(f)).digest('hex').slice(0, 8);
    }
  }
  const webDir = path.join(rootDir, 'webContext');
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(p);
      } else if (entry.isFile() && p.endsWith('.html')) {
        let html = fs.readFileSync(p, 'utf8');
        let changed = false;
        html = html.replace(/((?:src|href)=")(js\/[a-z0-9-]+\.min\.js|css\/[a-z0-9-]+\.min\.css)(\?v=[a-f0-9]+)?"/g, (m, pre, rel) => {
          const hash = hashMap[path.basename(rel)];
          if (!hash) {
            return m;
          }
          changed = true;
          return pre + rel + '?v=' + hash + '"';
        });
        if (changed) {
          fs.writeFileSync(p, html, 'utf8');
          console.log('[build] 版本戳: ' + path.relative(rootDir, p));
        }
      }
    }
  };
  walk(webDir);
}

async function build() {
  for (const target of jsTargets) {
    const r = await buildJs(target.name, target.modules);
    console.log('[build] ' + r.outFile + ' (' + r.sizeKB + ' KB, ' + r.modules + ' 模块)');
  }
  const css = buildCss();
  if (css) {
    console.log('[build] ' + css.outFile + ' (' + css.sizeKB + ' KB)');
  }
  stampVersions();
  console.log('[build] 全部完成');
}

build().catch((err) => {
  console.error('[build] 构建失败: ' + (err && err.stack ? err.stack : err));
  process.exit(1);
});
