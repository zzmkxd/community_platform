FROM elasticsearch:8.15.0
# IK 中文分词插件 — 国内网络可能下载失败，失败时跳过（无中文分词能力）
RUN elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.15.0 || echo "WARNING: IK analysis plugin skipped (download failed) — Chinese segmentation disabled"
