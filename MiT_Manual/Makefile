# Makefile for Sphinx documentation
#

.PHONY: help clean html pdf html-ko html-en pdf-ko pdf-en

help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  html       to make HTML files for all languages (ko, en)"
	@echo "  html-ko    to make HTML files for Korean"
	@echo "  html-en    to make HTML files for English"
	@echo "  pdf        to make PDF files for all languages (ko, en)"
	@echo "  pdf-ko     to make PDF file for Korean"
	@echo "  pdf-en     to make PDF file for English"
	@echo "  clean      to remove all build output"

clean:
	cd ko && $(MAKE) clean
	cd en && $(MAKE) clean

html: html-ko html-en

html-ko:
	cd ko && $(MAKE) html

html-en:
	cd en && $(MAKE) html

pdf: pdf-ko pdf-en

pdf-ko:
	cd ko && $(MAKE) pdf

pdf-en:
	cd en && $(MAKE) pdf

