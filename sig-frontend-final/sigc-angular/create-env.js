// create-env.js
const fs = require('fs');
const path = require('path');

const envDir = path.join(__dirname, 'sigc-angular/src/environments');

// Créer le dossier s'il n'existe pas
if (!fs.existsSync(envDir)) {
  fs.mkdirSync(envDir, { recursive: true });
  console.log('📁 Created environments directory');
}

// environment.prod.ts
const prodContent = `export const environment = {
  production: true,
  apiUrl: '${process.env.API_URL || '/api'}',
  backendUrl: '${process.env.BACKEND_URL || 'https://spring-pfe-ugzh.onrender.com'}'
};`;

fs.writeFileSync(path.join(envDir, 'environment.prod.ts'), prodContent);
console.log('✅ Created environment.prod.ts');

// environment.ts (dev)
const devContent = `export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  backendUrl: 'http://localhost:8080'
};`;

fs.writeFileSync(path.join(envDir, 'environment.ts'), devContent);
console.log('✅ Created environment.ts');
