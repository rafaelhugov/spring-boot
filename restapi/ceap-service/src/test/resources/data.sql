-- ============================================================
-- Configurações mínimas para inicialização (restapi-common)
-- ============================================================
INSERT INTO administrador.configuracao (id, propriedade, valor)
VALUES (1, 'global.instituicao.ip_email', 'localhost');

INSERT INTO administrador.configuracao (id, propriedade, valor)
VALUES (2, 'global.instituicao.porta_email', '25');



-- ============================================================
-- Dados de teste para Geografia (tj/public)
-- ============================================================
INSERT INTO public.municipio (id, nome, excluido) VALUES 
(1, 'João Pessoa', false),
(2, 'Campina Grande', false),
(3, 'Patos', false),
(4, 'Cabedelo', false),
(9, 'Sousa', false);

INSERT INTO tj.comarca (id, nome, id_municipio, excluido) VALUES 
(1, 'COMARCA DE JOAO PESSOA', 1, false),
(2, 'COMARCA DE CAMPINA GRANDE', 2, false),
(3, 'COMARCA DE PATOS', 3, false),
(4, 'COMARCA DE CABEDELO', 4, false),
(9, 'COMARCA DE SOUSA', 9, false);

-- ============================================================
-- Dados de teste para Entidades Policiais (CEAP-360)
-- ============================================================
INSERT INTO entidades (nomecnmp, descricaomppb, tipoentidade, id_comarca, municipio, ativa, criadoem) VALUES 
('1ª Delegacia Distrital - João Pessoa', '1ª DD João Pessoa', 'POLICIA_CIVIL', 1, 'João Pessoa', true, CURRENT_TIMESTAMP),
('2ª Delegacia Distrital - Campina Grande', '2ª DD Campina Grande', 'POLICIA_CIVIL', 2, 'Campina Grande', true, CURRENT_TIMESTAMP),
('Delegacia de Homicídios - Patos', 'DH Patos', 'POLICIA_CIVIL', 3, 'Patos', true, CURRENT_TIMESTAMP),
('Central de Polícia - Cabedelo', 'Central Cabedelo', 'POLICIA_CIVIL', 4, 'Cabedelo', true, CURRENT_TIMESTAMP);

-- ============================================================
-- Dados de teste para Membros / Servidores (Autocomplete)
-- ============================================================
-- Tipos de Cargo: M = Membro, S = Servidor
INSERT INTO recursoshumanos.tipocargo (id, tipo) VALUES (1, 'M');
INSERT INTO recursoshumanos.tipocargo (id, tipo) VALUES (2, 'S');

-- Funções / Cargos
INSERT INTO recursoshumanos.funcao (id, nome, id_tipocargo) VALUES (1, 'Promotor de Justiça', 1);
INSERT INTO recursoshumanos.funcao (id, nome, id_tipocargo) VALUES (2, 'Assessor Técnico', 2);

-- Pessoas
INSERT INTO public.pessoa (id, nome, email) VALUES (101, 'Marco Aurélio Silva', 'marco.aurelio@mppb.mp.br');
INSERT INTO public.pessoa (id, nome, email) VALUES (102, 'Maria Clara Ramos', 'maria.clara@mppb.mp.br');
INSERT INTO public.pessoa (id, nome, email) VALUES (103, 'João Silva Souza', 'joao.silva@mppb.mp.br');
INSERT INTO public.pessoa (id, nome, email) VALUES (104, 'Carlos Andrade Martins', 'carlos.andrade@mppb.mp.br');

-- Servidores (Tabela de vínculo corporativo)
-- matricula, ativo, excluido
INSERT INTO public.servidor (id, id_pessoa, id_cargo, matricula, ativo, excluido) VALUES (201, 101, 1, 'M9901', 'A', false); -- Marco (Membro Ativo)
INSERT INTO public.servidor (id, id_pessoa, id_cargo, matricula, ativo, excluido) VALUES (202, 102, 1, 'M9902', 'A', false); -- Maria (Membro Ativo)
INSERT INTO public.servidor (id, id_pessoa, id_cargo, matricula, ativo, excluido) VALUES (203, 103, 2, 'S8801', 'A', false); -- João (Servidor Ativo)
INSERT INTO public.servidor (id, id_pessoa, id_cargo, matricula, ativo, excluido) VALUES (204, 104, 1, 'M9903', 'I', false); -- Carlos (Membro Inativo)

