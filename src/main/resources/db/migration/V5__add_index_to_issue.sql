CREATE INDEX `ix_issue_created_id_desc`
  ON `issue` (`created_at` DESC, `id` DESC);

CREATE INDEX `ix_issue_label_issue_label` ON `issue_label` (`issue_id`, `label_id`);