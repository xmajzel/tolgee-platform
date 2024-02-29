import React, { useEffect, useState } from 'react';
import { T } from '@tolgee/react';
import { Box, IconButton, styled, TextField } from '@mui/material';
import { Send } from '@mui/icons-material';

import { useUser } from 'tg.globalContext/helpers';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { Comment } from './Comment';
import { useComments } from './useComments';
import { StickyDateSeparator } from 'tg.views/projects/translations/ToolsPanel/common/StickyDateSeparator';
import {
  PanelContentData,
  PanelContentProps,
  TranslationViewModel,
} from '../../common/types';
import { TabMessage } from '../../common/TabMessage';
import clsx from 'clsx';
import {
  StyledLoadMore,
  StyledLoadMoreButton,
} from '../../common/StyledLoadMore';
import { arraySplit } from '../../common/splitByParameter';

const StyledContainer = styled('div')`
  display: grid;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin-top: 8px;
`;

const StyledTextField = styled(TextField)`
  flex-grow: 1;
  margin-left: 8px;
  margin-right: 8px;
  margin-top: 8px;
  opacity: 0.5;
  &:focus-within {
    opacity: 1;
  }
  &:focus-within .icon-button {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

export const Comments: React.FC<PanelContentProps> = ({
  keyData,
  language,
  setItemsCount,
}) => {
  const { satisfiesPermission } = useProjectPermissions();
  const user = useUser();
  const [limit, setLimit] = useState(true);

  const canAddComment = satisfiesPermission('translation-comments.add');
  const canEditComment = satisfiesPermission('translation-comments.edit');
  const canSetCommentState = satisfiesPermission(
    'translation-comments.set-state'
  );

  const keyId = keyData.keyId;
  const translation = keyData.translations[language.tag] as
    | TranslationViewModel
    | undefined;

  const {
    commentsList,
    comments,
    handleAddComment,
    handleDelete,
    handleKeyDown,
    changeState,
    isAddingComment,
    inputValue,
    setInputValue,
    fetchMore,
  } = useComments({
    keyId,
    language,
    translation,
  });

  useEffect(() => {
    setItemsCount(translation?.commentCount);
  }, [translation?.commentCount]);

  const showLoadMore =
    comments.hasNextPage || (limit && commentsList.length > 4);

  function handleShowMore() {
    if (limit) {
      setLimit(false);
    } else {
      fetchMore();
    }
  }

  const trimmedComments = limit ? commentsList.slice(-4) : commentsList;

  const dayGroups = arraySplit(trimmedComments, (i) =>
    new Date(i.createdAt).toLocaleDateString()
  );

  return (
    <StyledContainer
      className={clsx({ commentsPresent: Boolean(trimmedComments.length) })}
    >
      {dayGroups.length !== 0 ? (
        dayGroups.map((items, gIndex) => (
          <Box key={gIndex} display="grid">
            <StickyDateSeparator date={new Date(items[0].createdAt)} />
            {items?.map((comment, cIndex) => {
              const canDelete =
                user?.id === comment.author.id || canEditComment;
              return (
                <React.Fragment key={comment.id}>
                  {showLoadMore && cIndex + gIndex === 0 && (
                    <StyledLoadMore>
                      <StyledLoadMoreButton
                        role="button"
                        onClick={handleShowMore}
                        data-cy="translations-comments-load-more-button"
                      >
                        <T keyName="translations_comments_previous_comments" />
                      </StyledLoadMoreButton>
                    </StyledLoadMore>
                  )}
                  <Comment
                    data={comment}
                    onDelete={canDelete ? handleDelete : undefined}
                    onChangeState={canSetCommentState ? changeState : undefined}
                  />
                </React.Fragment>
              );
            })}
          </Box>
        ))
      ) : (
        <TabMessage>
          <T keyName="translation_tools_nothing_found"></T>
        </TabMessage>
      )}

      {canAddComment && (
        <StyledTextField
          multiline
          variant="outlined"
          size="small"
          value={inputValue}
          onChange={(e) => setInputValue(e.currentTarget.value)}
          onKeyDown={handleKeyDown}
          data-cy="translations-comments-input"
          InputProps={{
            sx: {
              padding: '2px 4px 2px 12px',
            },
            endAdornment: (
              <IconButton
                className="icon-button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={handleAddComment}
                disabled={isAddingComment}
              >
                <Send fontSize="small" color="inherit" />
              </IconButton>
            ),
          }}
        />
      )}
    </StyledContainer>
  );
};

export const CommentsItemsCount = ({ keyData, language }: PanelContentData) => {
  const translation = keyData.translations[language.tag] as
    | TranslationViewModel
    | undefined;
  return <>{translation?.commentCount ?? 0}</>;
};
