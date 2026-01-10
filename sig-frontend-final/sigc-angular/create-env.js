// create-env.js
const fs = require('fs');
const path = require('path');

const envDir = path.join(__dirname, 'src', 'environments');

// Créer le dossier s'il n'existe pas
if (!fs.existsSync(envDir)) {
  fs.mkdirSync(envDir, { recursive: true });
  console.log('📁 Created environments directory');
}

// environment.prod.ts - SIMPLIFIÉ
const prodContent = `export const environment = {
  production: true,
  apiUrl: '/offer',
  backendUrl: 'https://spring-pfe-ugzh.onrender.com'
};`;

fs.writeFileSync(path.join(envDir, 'environment.prod.ts'), prodContent);
console.log('✅ Created environment.prod.ts at:', path.join(envDir, 'environment.prod.ts'));

// environment.ts (dev)
const devContent = `export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/offer',
  backendUrl: 'http://localhost:8080'
};`;

fs.writeFileSync(path.join(envDir, 'environment.ts'), devContent);
console.log('✅ Created environment.ts');

// Vérifier que les fichiers existent
console.log('Files in environments directory:');
fs.readdirSync(envDir).forEach(file => {
  console.log('  -', file);
});
