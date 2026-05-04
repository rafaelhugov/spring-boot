import puppeteer from 'puppeteer-core';
import path from 'path';
import { fileURLToPath } from 'url';
import { existsSync } from 'fs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const url = process.argv[2];
const outputPath = process.argv[3];

if (!url || !outputPath) {
    console.error('Uso: node pdf-engine.js <URL> <Caminho_Saida>');
    process.exit(1);
}

// Redireciona para o template HTML estático conforme o tipo de manual
function resolveTemplateUrl(sourceUrl) {
    const templates = {
        solicitante:    path.resolve(__dirname, '../public/pdf-template-solicitante.html'),
        administrativo: path.resolve(__dirname, '../public/pdf-template-administrativo.html'),
    };

    for (const [type, templatePath] of Object.entries(templates)) {
        if (sourceUrl.includes(`/manual/${type}`)) {
            if (existsSync(templatePath)) {
                console.log(`Usando template HTML: ${templatePath}`);
                return `file://${templatePath}`;
            }
            console.warn(`Template não encontrado para "${type}", renderizando o Vue App como fallback.`);
        }
    }

    return sourceUrl; // fallback: Vue App
}

(async () => {
    let browser;
    try {
        const targetUrl = resolveTemplateUrl(url);
        console.log(`Gerando PDF a partir de: ${targetUrl}`);

        browser = await puppeteer.launch({
            executablePath: process.env.PUPPETEER_EXECUTABLE_PATH || null,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-dev-shm-usage',
                '--disable-gpu',
                '--font-render-hinting=none'
            ]
        });

        const page = await browser.newPage();
        // Viewport mais largo que A4 para o template HTML renderizar sem quebras na fase de carregamento
        await page.setViewport({ width: 1200, height: 1123 });

        // Ativa print media ANTES de navegar — @media print faz body virar display:block
        // e remove o background escuro do html/body
        await page.emulateMediaType('print');

        await page.goto(targetUrl, { waitUntil: 'networkidle0', timeout: 90000 });

        // Aguarda carregamento completo de fontes (Google Fonts etc.)
        await page.evaluateHandle('document.fonts.ready');
        await new Promise(resolve => setTimeout(resolve, 2000));

        console.log('Gerando arquivo PDF...');
        await page.pdf({
            path: outputPath,
            format: 'A4',
            printBackground: true,
            displayHeaderFooter: false,
            margin: { top: '0', right: '0', bottom: '0', left: '0' }
        });

        console.log(`PDF gerado com sucesso em: ${outputPath}`);
    } catch (error) {
        console.error('Erro ao gerar PDF:', error);
        process.exit(1);
    } finally {
        if (browser) await browser.close();
    }
})();
