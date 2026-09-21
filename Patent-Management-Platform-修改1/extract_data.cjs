const fs = require('fs');
const path = 'd:/Patent-Management-Platform/例子/dist/assets/Home-65c28f0b.js';
const content = fs.readFileSync(path, 'utf-8');

function findAndPrint(keyword, context = 500) {
    const index = content.indexOf(keyword);
    if (index !== -1) {
        console.log(`--- Context for "${keyword}" ---`);
        const start = Math.max(0, index - context);
        const end = Math.min(content.length, index + context + keyword.length);
        console.log(content.substring(start, end));
        console.log('------------------------------');
    } else {
        console.log(`Keyword "${keyword}" not found.`);
    }
}

findAndPrint('官方邮箱');
findAndPrint('status:"已完成"');
findAndPrint('amount:');
