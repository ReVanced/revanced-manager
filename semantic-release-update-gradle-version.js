const fs = require('fs');
const path = require('path');

// Use semantic-release injected env var or fallback
const version = process.env.npm_package_nextRelease_version || process.env.VERSION;

if (!version) {
  console.error('❌ No version provided in env (npm_package_nextRelease_version or VERSION)');
  process.exit(1);
}

const filePath = path.join(process.cwd(), 'gradle.properties');

if (!fs.existsSync(filePath)) {
  console.error(`❌ gradle.properties not found in ${filePath}`);
  process.exit(1);
}

let content = fs.readFileSync(filePath, 'utf8');

// Replace version with or without spaces
const versionRegex = /^version\s*=\s*.*/m;

if (versionRegex.test(content)) {
  content = content.replace(versionRegex, `version=${version}`);
} else {
  content += `\nversion=${version}`;
}

fs.writeFileSync(filePath, content, 'utf8');

console.log(`✔ [${process.cwd()}] Updated version to ${version}`);
