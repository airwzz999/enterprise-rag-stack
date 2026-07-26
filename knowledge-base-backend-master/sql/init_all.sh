#!/bin/bash

# =====================================================
# Enterprise Knowledge Base System - one-click microservice database initialization script
# =====================================================

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database configuration
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASS="123456"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Enterprise Knowledge Base System - Database Initialization${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check MySQL connection
echo -e "${YELLOW}[1/10] Checking MySQL connection...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} -e "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ MySQL connection successful${NC}"
else
    echo -e "${RED}✗ MySQL connection failed, please check the configuration${NC}"
    exit 1
fi

# Create all databases
echo ""
echo -e "${YELLOW}[2/10] Creating all databases...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} < sql/00_create_databases.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Databases created successfully${NC}"
else
    echo -e "${RED}✗ Database creation failed${NC}"
    exit 1
fi

# Create kb_user tables
echo ""
echo -e "${YELLOW}[3/10] Creating kb_user database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_user < sql/01_kb_user.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_user tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_user table creation failed${NC}"
fi

# Create kb_document tables
echo ""
echo -e "${YELLOW}[4/10] Creating kb_document database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_document < sql/02_kb_document.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_document tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_document table creation failed${NC}"
fi

# Create kb_search tables
echo ""
echo -e "${YELLOW}[5/10] Creating kb_search database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_search < sql/03_kb_search.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_search tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_search table creation failed${NC}"
fi

# Create kb_file tables
echo ""
echo -e "${YELLOW}[6/10] Creating kb_file database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_file < sql/04_kb_file.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_file tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_file table creation failed${NC}"
fi

# Create kb_ai tables
echo ""
echo -e "${YELLOW}[7/10] Creating kb_ai database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_ai < sql/05_kb_ai.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_ai tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_ai table creation failed${NC}"
fi

# Create kb_statistics tables
echo ""
echo -e "${YELLOW}[8/10] Creating kb_statistics database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_statistics < sql/06_kb_statistics.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_statistics tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_statistics table creation failed${NC}"
fi

# Create kb_notification tables
echo ""
echo -e "${YELLOW}[9/10] Creating kb_notification database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_notification < sql/07_kb_notification.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_notification tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_notification table creation failed${NC}"
fi

# Create kb_graph and kb_common tables
echo ""
echo -e "${YELLOW}[10/10] Creating kb_graph and kb_common database tables...${NC}"
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_graph < sql/08_kb_graph.sql
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} kb_common < sql/09_kb_common.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ kb_graph and kb_common tables created successfully${NC}"
else
    echo -e "${RED}✗ kb_graph and kb_common table creation failed${NC}"
fi

# Display summary information
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Database Initialization Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS} -e "
SELECT 'Database list' AS info;
SHOW DATABASES LIKE 'kb_%';
"

echo ""
echo -e "${GREEN}✓ All databases and tables created successfully!${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo -e "1. Run the seed data scripts"
echo -e "2. Start each microservice"
echo ""
