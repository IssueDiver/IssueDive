-- V3__add_added_at_to_issue_label.sql
ALTER TABLE issue_label ADD COLUMN added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;