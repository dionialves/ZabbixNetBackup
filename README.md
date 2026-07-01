# ZabbixNetBackup

O **ZabbixNetBackup** (comando `znb`) é uma ferramenta de linha de comando que automatiza o backup de dispositivos de rede (roteadores, switches, OLTs) utilizando o [Zabbix](https://www.zabbix.com/) como fonte central de inventário.

A ferramenta conecta-se à API do Zabbix, consulta hosts pertencentes a um host group e, via SSH, executa os comandos de backup específicos de cada fabricante (Mikrotik, Cisco, Datacom, Digistar, Mimosa, Ubiquiti), armazenando localmente o resultado.

## Funcionalidades

- **Backup por fabricante** — um subcomando por vendor, com argumentos próprios:
  `mikrotik`, `cisco`, `datacom`, `digistar`, `mimosa`, `ubiquiti`
- **Inventário via Zabbix** — dispositivos obtidos por host group ID
- **Configuração isolada** — credenciais do Zabbix guardadas em `~/.znb/config`
- **Senha segura** — a senha do dispositivo é pedida de forma interativa, sem ecoar no terminal
- **Executável único** — um único arquivo `znb` (self-executable jar) que roda como `rm` ou `mv`

## Pré-requisitos

- **Java 21** ou superior (para build e execução)
- **Maven** (apenas para build; o script de instalação cuida disso)
- Acesso a um servidor Zabbix com API habilitada

## Instalação

A instalação é feita com um único comando: copie e cole o bloco abaixo no terminal.

### macOS / Linux

```bash
curl -fsSL https://raw.githubusercontent.com/dionialves/ZabbixNetBackup/main/install.sh | bash
```

> O script de instalação executa os seguintes passos automaticamente:
> 1. Verifica se o Java 21 está instalado; se não, exibe instruções de instalação e aborta
> 2. Clona o repositório para um diretório temporário
> 3. Compila o projeto com Maven (`mvn clean package`)
> 4. Gera o executável `znb` (self-executable jar) em `target/`
> 5. Instala `znb` em `~/.local/bin/`
> 6. Remove os arquivos clonados (limpeza)
> 7. Instrui como adicionar `~/.local/bin` ao `PATH`, se necessário

### Adicionar `~/.local/bin` ao PATH

Se `~/.local/bin` ainda não estiver no seu `PATH`, adicione-o ao arquivo de configuração do shell:

**zsh** (macOS padrão):
```bash
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**bash**:
```bash
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

**fish**:
```bash
fish_add_path ~/.local/bin
```

## Uso

Após a instalação, o `znb` pode ser chamado diretamente:

```bash
znb --help              # Lista os comandos disponíveis
znb init                # Configura credenciais do Zabbix (interativo)
znb backup --help       # Lista os fabricantes suportados
```

### Configuração inicial

O comando `init` pede interativamente a URL, usuário e senha da API do Zabbix e salva em `~/.znb/config`:

```bash
znb init
```

Para sobrescrever uma configuração existente:

```bash
znb init --force
```

### Executando um backup

Sintaxe geral:

```bash
znb backup <fabricante> -u <usuario> -p -g <group-id>
```

Exemplo — backup de Mikrotiks do host group `209`:

```bash
znb backup mikrotik -u admin -p -g 209
```

- `-u` / `--username` — usuário SSH do dispositivo
- `-p` / `--password` — senha (pedida interativamente, sem ecoar)
- `-g` / `--group-id` — ID do host group no Zabbix
- `-P` / `--ssh-port` — porta SSH (default: `22`)

Fabricantes suportados (subcomandos de `backup`):

| Comando    | Fabricante |
|------------|------------|
| `mikrotik`  | Mikrotik   |
| `cisco`    | Cisco      |
| `datacom`  | Datacom    |
| `digistar` | Digistar   |
| `mimosa`   | Mimosa     |
| `ubiquiti` | Ubiquiti   |

## Desinstalação

O `znb` não cria serviços, variáveis de ambiente globais nem arquivos fora do diretório pessoal do usuário. Para remover tudo:

1. **Remova o executável:**
   ```bash
   rm -f ~/.local/bin/znb
   ```

2. **Remova a configuração e os backups** (opcional):
   ```bash
   rm -rf ~/.znb
   ```

3. **Remova a entrada do `PATH`** (se adicionada durante a instalação):
   ```bash
   # zsh
   sed -i '' '/\.local\/bin/d' ~/.zshrc

   # bash (Linux/macOS)
   sed -i '/\.local\/bin/d' ~/.bashrc
   ```

> Caso tenha instalado o Java 21 **apenas** para usar o `znb` e não precise mais dele, remova-o com o mesmo gerenciador usado na instalação (`brew uninstall openjdk@21`, `sudo apt-get remove openjdk-21-jdk`, etc.).

## Atualização

Para atualizar para a versão mais recente, basta executar novamente o script de instalação:

```bash
curl -fsSL https://raw.githubusercontent.com/dionialves/ZabbixNetBackup/main/install.sh | bash
```

Ele sobrescreve o `znb` existente em `~/.local/bin/`.

## Desenvolvimento

### Estrutura do projeto

```
src/main/java/com/dionialves/
├── Main.java                 # Entry point (picocli)
├── cli/                       # Comandos CLI (backup, init, vendors)
├── core/
│   ├── integration/           # ZabbixClient (API)
│   ├── service/               # Serviços de backup por fabricante
│   ├── util/                  # Utilitários (ProgressBar)
│   └── exception/             # Exceções específicas
└── model/                     # Modelos (BackupResult, BackupSummary)
```

### Build manual

```bash
mvn clean package
```

Gera em `target/`:
- `zabbix-net-backup.jar` — jar standalone (`java -jar`)
- `znb` — executável self-contained (cabeçalho shell + jar)

### Tecnologias

- Java 21
- Maven (build) + shade plugin (fat jar) + antrun (executável `znb`)
- [picocli](https://picocli.info/) — CLI framework
- [sshj](https://github.com/hierynomus/sshj) / [jsch](https://github.com/mwiede/jsch) — SSH
- [zabbix-api](https://github.com/hengyunabc/zabbix-api) — cliente da API do Zabbix
- [gson](https://github.com/google/gson) — JSON
- [dotenv-java](https://github.com/cdimascio/dotenv-java) — variáveis de ambiente

## Licença

Distribuído sob licença MIT. Veja `LICENSE` para detalhes.